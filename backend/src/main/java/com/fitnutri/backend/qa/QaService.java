package com.fitnutri.backend.qa;

import com.fitnutri.backend.ai.AiFeature;
import com.fitnutri.backend.capabilities.CapabilitiesService;
import com.fitnutri.backend.user.User;
import org.springframework.stereotype.Service;

/** No enclosing transaction. Each eligibility read and recorded summary completes before the external call. */
@Service
public final class QaService {
 public static final String DISCLAIMER="General information and recorded-data summaries only, not medical advice. Consult a qualified health professional for personal advice. Recorded data may be incomplete; no calorie allowance or treatment is prescribed.";
 private final QaEligibilityService eligibility;private final CapabilitiesService capabilities;private final QaScopePolicy policy;private final QaContextBuilder context;private final OpenAiQaAdapter model;
 public QaService(QaEligibilityService eligibility,CapabilitiesService capabilities,QaScopePolicy policy,QaContextBuilder context,OpenAiQaAdapter model){this.eligibility=eligibility;this.capabilities=capabilities;this.policy=policy;this.context=context;this.model=model;}
 public QaResponse ask(User owner,QaRequest request){
  eligibility.requireEligibility(owner.getId()); // must precede generic unconfigured503
  capabilities.requireAvailable(AiFeature.QA,owner);
  context.validateRange(request.recordRange(),request.zone());var decision=policy.assessScope(request.message(),request.recordRange()!=null);
  if(decision.scope()==QaScopePolicy.Scope.REFUSED){eligibility.requireEligibility(owner.getId());return new QaResponse(decision.explanation(),DISCLAIMER,QaScopePolicy.Scope.REFUSED,null,null);}
  var facts=request.recordRange()==null?null:context.buildAllowedFacts(owner.getId(),request.recordRange(),request.zone());
  eligibility.requireEligibility(owner.getId()); // withdrawal during context construction prevents dispatch
  var answer=model.answer(request.message(),decision.scope(),facts);
  eligibility.requireEligibility(owner.getId()); // a legitimate earlier transmission cannot be undone, but no answer escapes
  return new QaResponse(answer.answer(),DISCLAIMER,answer.scope(),facts==null?null:request.recordRange(),null);
 }
}
