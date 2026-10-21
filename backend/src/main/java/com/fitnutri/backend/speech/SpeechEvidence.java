package com.fitnutri.backend.speech;

import com.fitnutri.backend.ai.AiExceptionMapper;
import com.fitnutri.backend.common.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.regex.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Literal evidence, dates and units are checked independently of model numeric/date claims. */
@Component
public final class SpeechEvidence {
    private static final Pattern DATES=Pattern.compile("\\b[0-9]{4}-[0-9]{2}-[0-9]{2}\\b|\\b(?:today|yesterday|tomorrow)\\b|今天|昨天|明天",Pattern.CASE_INSENSITIVE);
    private static final String NUMBER="([0-9]{1,6}(?:\\.[0-9]{1,6})?)";
    private final RecordDatePolicy dates;
    public SpeechEvidence(RecordDatePolicy dates){this.dates=dates;}
    public void requireContext(LocalDate baseDate,String zone){
        if(zone==null||zone.isBlank())throw BusinessException.invalid("zone","Choose a supported IANA time zone.");
        LocalDate today=dates.today(zone);if(!today.equals(baseDate))throw new BusinessException(HttpStatus.CONFLICT,"DATE_CONTEXT_CHANGED","Refresh the current date before extracting this transcript.",List.of(new FieldViolation("baseDate",null,"The server current date is "+today+" in "+zone+".")));
    }
    public DateResult resolveRelativeDate(String transcript,String mention,LocalDate baseDate,String zone){
        requireContext(baseDate,zone);var found=new LinkedHashSet<LocalDate>();var matches=DATES.matcher(transcript);while(matches.find())found.add(parseDate(matches.group(),baseDate));
        if(mention!=null){requireLiteral(transcript,mention);found.add(parseDate(mention,baseDate));}
        if(found.size()>1)throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"SPLIT_DATED_ENTRY","This entry mentions different dates. Split it into separate entries before extraction.");
        LocalDate value=found.isEmpty()?baseDate:found.iterator().next();dates.requireDate(value,zone);return new DateResult(value,found.isEmpty());
    }
    private LocalDate parseDate(String mention,LocalDate base){
        return switch(mention.strip().toLowerCase(Locale.ROOT)){case "today","今天"->base;case "yesterday","昨天"->base.minusDays(1);case "tomorrow","明天"->base.plusDays(1);default->{try{if(!mention.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new DateTimeException("unsupported date phrase");yield LocalDate.parse(mention);}catch(DateTimeException ignored){throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,"DATE_REVIEW_REQUIRED","Use today, yesterday or an explicit YYYY-MM-DD date, or continue manually.");}}};
    }
    public String requireLiteral(String transcript,String value){if(value==null||value.isBlank()||value.length()>100||value.codePoints().anyMatch(Character::isISOControl)||!transcript.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT)))throw AiExceptionMapper.badResponse();return value.strip();}
    public BigDecimal grams(String value){if(value==null)return null;var match=Pattern.compile("^"+NUMBER+"\\s*(?:g|grams?|克)$",Pattern.CASE_INSENSITIVE).matcher(value.strip());return match.matches()?quantity(new BigDecimal(match.group(1)),10000,3):null;}
    public BigDecimal minutes(String value){
        if(value==null)return null;String text=value.strip().toLowerCase(Locale.ROOT);
        if(Set.of("half an hour","half a hour","half hour","half-hour","半小时").contains(text))return new BigDecimal("30");
        if(Set.of("a quarter of an hour","quarter of an hour","quarter hour","一刻钟").contains(text))return new BigDecimal("15");
        var minute=Pattern.compile("^"+NUMBER+"\\s*(?:minutes?|mins?|分钟)$").matcher(text);if(minute.matches())return quantity(new BigDecimal(minute.group(1)),1440,2);
        var hour=Pattern.compile("^"+NUMBER+"\\s*(?:hours?|hrs?|小时)(?:\\s*"+NUMBER+"\\s*(?:minutes?|mins?|分钟))?$").matcher(text);if(hour.matches()){BigDecimal result=new BigDecimal(hour.group(1)).multiply(new BigDecimal("60"));if(hour.group(2)!=null)result=result.add(new BigDecimal(hour.group(2)));return quantity(result,1440,2);}return null;
    }
    private BigDecimal quantity(BigDecimal value,int maximum,int scale){return value.signum()>0&&value.compareTo(BigDecimal.valueOf(maximum))<=0&&value.stripTrailingZeros().scale()<=scale?value:null;}
    public record DateResult(LocalDate recordDate,boolean dateWasDefaulted){}
}
