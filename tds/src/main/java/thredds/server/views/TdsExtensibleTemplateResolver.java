package thredds.server.views;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring4.templateresource.SpringResourceTemplateResource;
import org.thymeleaf.templateresource.FileTemplateResource;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.util.StringUtils;
import org.thymeleaf.util.Validate;
import thredds.server.config.TdsContext;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class TdsExtensibleTemplateResolver extends SpringResourceTemplateResolver implements ApplicationContextAware {

  private static final String EXT_FRAG_PREFIX = "ext:";
  private static final int PREFIX_LENGTH = EXT_FRAG_PREFIX.length();

  private ApplicationContext applicationContext = null;


  protected TdsExtensibleTemplateResolver() {
    super();
    this.setCheckExistence(true);
    Set<String> resolvablePatterns = new HashSet<>();
    resolvablePatterns.add("ext:*");
    this.setResolvablePatterns(resolvablePatterns);
  }

  @Override
  protected String computeResourceName(
          final IEngineConfiguration configuration, final String ownerTemplate, final String template,
          final String prefix, final String suffix, final Map<String, String> templateAliases,
          final Map<String, Object> templateResolutionAttributes) {

    Validate.notNull(template, "Template name cannot be null");

    // Don't bother computing resource name if template is not extensible
    if (!template.startsWith(EXT_FRAG_PREFIX)) return template;

    String resourceName = template.substring(PREFIX_LENGTH);
    if (!StringUtils.isEmptyOrWhitespace(prefix)) resourceName = prefix + resourceName;
    if (!StringUtils.isEmptyOrWhitespace(suffix)) resourceName = resourceName + suffix;

    TdsContext tdsContext = (TdsContext)applicationContext.getBean("TdsContext");
    resourceName = tdsContext.getThreddsDirectory() + resourceName;


    return resourceName;
  }

  @Override
  protected ITemplateResource computeTemplateResource(
          final IEngineConfiguration configuration, final String ownerTemplate, final String template, final String resourceName, final String characterEncoding, final Map<String, Object> templateResolutionAttributes) {
      return new FileTemplateResource(resourceName, characterEncoding);
  }

  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }


}
