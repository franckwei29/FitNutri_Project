package com.fitnutri.backend.qa;

import com.fitnutri.backend.ai.AiFeature;
import com.fitnutri.backend.capabilities.CapabilitiesService;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class QaServiceTest {
 final QaEligibilityService eligibility=mock(QaEligibilityService.class);final CapabilitiesService capabilities=mock(CapabilitiesService.class);final QaContextBuilder context=mock(QaContextBuilder.class);final OpenAiQaAdapter model=mock(OpenAiQaAdapter.class);final QaService service=new QaService(eligibility,capabilities,new QaScopePolicy(),context,model);
 User owner(){var u=new User();u.setId(41L);return u;}QaRequest request(){return new QaRequest("General walking information",null,"Australia/Sydney");}BusinessException restricted(){return new BusinessException(HttpStatus.FORBIDDEN,"QA_RESTRICTED","Profile eligibility changed.");}
 @Test void eligibilityIsCheckedBeforeGenericMissingModelConfiguration(){doThrow(restricted()).when(eligibility).requireEligibility(41L);assertEquals("QA_RESTRICTED",assertThrows(BusinessException.class,()->service.ask(owner(),request())).code());verifyNoInteractions(capabilities,context,model);}
 @Test void policyRefusalNeverBuildsPersonalContextOrCallsAProvider(){var response=service.ask(owner(),new QaRequest("Can you diagnose my knee pain?",null,"Australia/Sydney"));assertEquals(QaScopePolicy.Scope.REFUSED,response.scope());assertNull(response.recordRangeUsed());verify(context,never()).buildAllowedFacts(anyLong(),any(),anyString());verifyNoInteractions(model);verify(eligibility,times(2)).requireEligibility(41L);}
 @Test void crisisRefusalNeverTransmitsToProvider(){var response=service.ask(owner(),new QaRequest("I want to die",null,"Australia/Sydney"));assertEquals(QaScopePolicy.Scope.REFUSED,response.scope());assertEquals(QaScopePolicy.CRISIS_REFUSAL,response.answer());verifyNoInteractions(model);verify(context,never()).buildAllowedFacts(anyLong(),any(),anyString());}
 @Test void successfulSingleTurnChecksEligibilityAgainBeforeDispatchAndOutput(){when(model.answer(anyString(),any(),isNull())).thenReturn(new OpenAiQaAdapter.Answer(QaScopePolicy.Scope.GENERAL,"Synthetic controlled answer only."));var response=service.ask(owner(),request());assertEquals(QaScopePolicy.Scope.GENERAL,response.scope());assertFalse(response.disclaimer().isBlank());assertNull(response.recordRangeUsed());var order=inOrder(eligibility,model);order.verify(eligibility,times(2)).requireEligibility(41L);order.verify(model).answer(anyString(),any(),isNull());order.verify(eligibility).requireEligibility(41L);}
 @Test void committedWithdrawalBeforeDispatchPreventsExternalTransmission(){doNothing().doThrow(restricted()).when(eligibility).requireEligibility(41L);assertEquals("QA_RESTRICTED",assertThrows(BusinessException.class,()->service.ask(owner(),request())).code());verifyNoInteractions(model);}
 @Test void withdrawalAfterProviderReturnsSuppressesItsEntireAnswer(){when(model.answer(anyString(),any(),isNull())).thenReturn(new OpenAiQaAdapter.Answer(QaScopePolicy.Scope.GENERAL,"PRIVATE_ANSWER_MUST_NOT_ESCAPE"));doNothing().doNothing().doThrow(restricted()).when(eligibility).requireEligibility(41L);var error=assertThrows(BusinessException.class,()->service.ask(owner(),request()));assertEquals("QA_RESTRICTED",error.code());assertFalse(error.getMessage().contains("PRIVATE_ANSWER"));verify(model).answer(anyString(),any(),isNull());}
 @Test void providerFailureIsPropagatedWithoutRetryOrFixedAdvice(){when(model.answer(anyString(),any(),isNull())).thenThrow(new BusinessException(HttpStatus.BAD_GATEWAY,"PROVIDER_BAD_RESPONSE","No usable provider response."));assertEquals("PROVIDER_BAD_RESPONSE",assertThrows(BusinessException.class,()->service.ask(owner(),request())).code());verify(model,times(1)).answer(anyString(),any(),isNull());verify(capabilities).requireAvailable(eq(AiFeature.QA),argThat(u->u.getId().equals(41L)));}
}
