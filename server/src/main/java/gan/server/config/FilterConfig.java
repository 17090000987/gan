package gan.server.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import gan.server.web.CaseInsensitiveRequestParameterNameFilter;

@Configuration
public class FilterConfig {

	@Bean
	public FilterRegistrationBean caseInsensitiveRequestParameterNameFilter() {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(new CaseInsensitiveRequestParameterNameFilter());
		registration.addUrlPatterns("/*");// 拦截路径
		registration.setName(CaseInsensitiveRequestParameterNameFilter.class.getName());// 拦截器名称
		registration.setOrder(2);// 顺序
		return registration;
	}

}