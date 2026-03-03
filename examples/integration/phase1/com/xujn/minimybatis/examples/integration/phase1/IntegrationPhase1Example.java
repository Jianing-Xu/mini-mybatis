package com.xujn.minimybatis.examples.integration.phase1;

import com.xujn.minispring.context.support.AnnotationConfigApplicationContext;
import java.util.List;

/**
 * Runnable example that exercises mini-mybatis integration with mini-spring.
 *
 * <p>Responsibility: provide a manual acceptance entry point for mapper scan,
 * bean registration and query execution through the container.
 *
 * <p>Thread-safety: single-threaded demo entrypoint.
 */
public final class IntegrationPhase1Example {

    private IntegrationPhase1Example() {
    }

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext("com.xujn.minimybatis.examples.integration.phase1");

        IntegrationUserService userService = context.getBean(IntegrationUserService.class);
        IntegrationUserMapper mapper = context.getBean(IntegrationUserMapper.class);

        IntegrationUser user = userService.load(1L);
        List<IntegrationUser> users = mapper.selectAll();

        if (user == null || users.size() != 3) {
            throw new IllegalStateException("Integration example verification failed");
        }

        System.out.println("integration selectById -> " + user);
        System.out.println("integration selectAll size -> " + users.size());
    }
}
