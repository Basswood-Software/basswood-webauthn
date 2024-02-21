package io.basswood.authenticator;

import brave.context.log4j2.ThreadContextScopeDecorator;
import io.basswood.authenticator.exception.GlobalErrorHandler;
import io.basswood.authenticator.rest.DeviceController;
import io.basswood.authenticator.service.DeviceDAO;
import io.basswood.authenticator.service.DeviceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author shamualr
 * @since 1.0
 */
@Configuration
@ComponentScan(excludeFilters = {@ComponentScan.Filter(RestController.class),
        @ComponentScan.Filter(ControllerAdvice.class)})
// Exclude RestControllers as they will be created explicitly via the @Bean methods
public class MainConfiguration {

    //Controllers
    @Bean
    public GlobalErrorHandler globalErrorHandler() {
        return new GlobalErrorHandler();
    }


    @Bean
    public ThreadContextScopeDecorator threadContextScopeDecorator() {
        return new ThreadContextScopeDecorator();
    }

    @Bean
    public DeviceDAO deviceDAO() {
        return new DeviceDAO();
    }

    @Bean
    public DeviceService deviceService() {
        return new DeviceService(deviceDAO());
    }

    @Bean
    public DeviceController deviceController() {
        return new DeviceController(deviceService());
    }
}
