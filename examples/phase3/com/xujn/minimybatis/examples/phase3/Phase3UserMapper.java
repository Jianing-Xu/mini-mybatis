package com.xujn.minimybatis.examples.phase3;

import java.util.List;

/**
 * Example mapper for phase 3 executor extension scenarios.
 *
 * <p>Responsibility: drive repeated same-SQL queries and different-SQL queries
 * so statement reuse behavior can be observed without changing mapper APIs.
 *
 * <p>Thread-safety: interface only; implementation follows session scope.
 */
public interface Phase3UserMapper {

    Phase3User selectById(Long id);

    List<Phase3User> selectByEmail(String email);

    List<Phase3User> selectAll();
}
