package com.fitnutri.backend.qa;

import com.fitnutri.backend.common.BusinessException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Initial conservative rules only, not an exhaustive medical/injection detector. Provider and output boundaries remain required. */
@Component
public final class QaScopePolicy {
 public enum Scope {GENERAL,RECORD_SUMMARY,REFUSED}
 public record Decision(Scope scope,String explanation){}
 public static final String MEDICAL_REFUSAL="This assistant provides general wellbeing information and recorded-data summaries. It cannot diagnose conditions, recommend medication or provide treatment. Consult a qualified health professional for personal medical questions.";
 private static final Pattern CRISIS=Pattern.compile("suicid|kill myself|self[- ]harm|hurt myself|want to die|end my life");
 public static final String CRISIS_REFUSAL="This assistant cannot help with a crisis. If you are in immediate danger, contact local emergency services now. Reach out to someone you trust or a qualified crisis support service.";
 private static final Pattern MEDICAL=Pattern.compile("\\b(?:diagnos\\w*|symptoms?|medications?|medicines?|dosage|doses?|prescri\\w*|treatment\\w*|therap\\w*|disease\\w*|diabetes|insulin|hypertension|cancer|pregnan\\w*|injur\\w*|surgery|surgical|pain|fever|dizz\\w*|nausea|antibiotic\\w*)\\b|eating disorder|anorexia|bulimia|blood pressure|side effects?|medical advice|诊断|症状|用药|药物|剂量|处方|治疗|治愈|疼痛|胸痛|糖尿病|高血压|怀孕|孕妇|胰岛素|病史|副作用|发烧|头晕|呕吐|过敏");
 private static final Pattern OVERRIDE=Pattern.compile("ignore.{0,80}(?:instructions?|rules?)|(?:system|developer)\\s*(?:prompt|message|instructions)|\\bsql\\b|execute.{0,30}(?:tool|code)|(?:delete|drop).{0,30}(?:database|table)|act as.{0,30}(?:doctor|physician)|忽略.{0,30}(?:指令|规则)|系统提示|删除数据库");
 private static final Pattern PRIVATE=Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}|\\b(?:password|api key|access token)\\b|other users?|another account|user\\s*id|其他用户|别人.{0,10}记录|密码|密钥");
 private static final Pattern PRESCRIPTION=Pattern.compile("(?:my|personalized|personalised).{0,25}(?:meal|diet|training|workout).{0,12}plan|how many (?:calories|kilojoules|kj).{0,60}should i|饮食处方|训练处方|减肥食谱|我(?:该|应该)吃多少");
 private static final Pattern RECORDS=Pattern.compile("\\bmy\\b.{0,50}(?:record|log|meal|food|calorie|activity|exercise)|what have i.{0,50}(?:record|log|eat|eaten)|我的.{0,20}(?:记录|饮食|运动)|记录.{0,10}(?:这周|本周)");
 public Decision assessScope(String message,boolean hasRecordRange){
  if(message==null||message.isBlank()||message.length()>2000||message.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw BusinessException.invalid("message","Enter 1 to 2,000 characters of question text.");
  return assessNormalized(message,hasRecordRange);
 }
 public Decision assessGeneratedAnswer(String answer,boolean hasRecordRange){return assessNormalized(answer,hasRecordRange);}
 private Decision assessNormalized(String message,boolean hasRecordRange){
  String text=Normalizer.normalize(message,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("\\p{Cf}","").replaceAll("\\s+"," ");
  if(CRISIS.matcher(text).find())return new Decision(Scope.REFUSED,CRISIS_REFUSAL);
  if(PRIVATE.matcher(text).find())return new Decision(Scope.REFUSED,"Ask about general wellbeing or your own recorded summaries. Remove personal identifiers, secrets and requests for other accounts' data.");
  if(MEDICAL.matcher(text).find())return new Decision(Scope.REFUSED,MEDICAL_REFUSAL);
  if(PRESCRIPTION.matcher(text).find())return new Decision(Scope.REFUSED,"This assistant can explain general habits and summarize records, but cannot prescribe individual calorie intakes or diet and training plans.");
  if(OVERRIDE.matcher(text).find())return new Decision(Scope.REFUSED,"Only general wellbeing information and your own recorded summaries are available. Instructions cannot change permissions or enable tools.");
  if(!hasRecordRange&&RECORDS.matcher(text).find())return new Decision(Scope.REFUSED,"Choose an explicit record range of up to 31 days to ask about your recorded data. No records were sent for this question.");
  return new Decision(hasRecordRange?Scope.RECORD_SUMMARY:Scope.GENERAL,null);
 }
}
