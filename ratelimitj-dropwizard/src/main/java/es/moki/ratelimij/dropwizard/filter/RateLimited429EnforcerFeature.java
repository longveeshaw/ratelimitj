package es.moki.ratelimij.dropwizard.filter;

import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import org.glassfish.jersey.server.model.AnnotatedMethod;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class RateLimited429EnforcerFeature implements DynamicFeature {

    @Override
    public void configure(final ResourceInfo resourceInfo,
                          final FeatureContext context) {

        final AnnotatedMethod method = new AnnotatedMethod(resourceInfo.getResourceMethod());
        final RateLimited rateLimited = method.getAnnotation(RateLimited.class);

        if (null != rateLimited) {
            context.register(RateLimit429EnforcerFilter.class);
        }
    }
}
