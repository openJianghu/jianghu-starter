package org.jianghu.app.config.jinjava;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.expression.ExpressionStrategy;
import com.hubspot.jinjava.lib.filter.EscapeFilter;
import com.hubspot.jinjava.objects.SafeString;
import com.hubspot.jinjava.tree.output.RenderedOutputNode;
import com.hubspot.jinjava.tree.parse.ExpressionToken;

@Slf4j
public class CustomExpressionStrategy implements ExpressionStrategy {

  public static final String ECHO_UNDEFINED = "echoUndefined";

  public RenderedOutputNode interpretOutput(ExpressionToken master, JinjavaInterpreter interpreter) {
    Object var = interpreter.resolveELExpression(master.getExpr(), master.getLineNumber());
    final com.hubspot.jinjava.features.FeatureActivationStrategy feat = interpreter
        .getConfig()
        .getFeatures()
        .getActivationStrategy(ECHO_UNDEFINED);

    if (var == null && feat.isActive(interpreter.getContext())) {
      return new RenderedOutputNode(master.getImage());
    }

    String result = interpreter.getAsString(var);

    if (interpreter.getConfig().isNestedInterpretationEnabled()) {
      if (!StringUtils.equals(result, master.getImage()) &&
              (StringUtils.contains(result, master.getSymbols().getExpressionStart()) || StringUtils.contains(result, master.getSymbols().getExpressionStartWithTag()))) {
        result = interpreter.renderFlat(result);
      }
    }

    if (interpreter.getContext().isAutoEscape() && !(var instanceof SafeString)) {
      result = EscapeFilter.escapeHtmlEntities(result);
    }

    return new RenderedOutputNode(result);
  }
}
