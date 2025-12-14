package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.SystemConfigAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统配置审计日志数据访问层
 *
 * @author System
 * @since 2025-12-14
 */
@Mapper
public interface SystemConfigAuditRepository extends BaseMapper<SystemConfigAudit> {

    /**
     * 根据配置键查询审计日志
     *
     * @param configKey 配置键
     * @param limit 限制条数
     * @return 审计日志列表
     */
    @Select("SELECT * FROM system_config_audit WHERE config_key = #{configKey} ORDER BY operation_time DESC LIMIT #{limit}")
    List<SystemConfigAudit> findByConfigKeyOrderByOperationTimeDesc(@Param("configKey") String configKey, @Param("limit") int limit);

    /**
     * 根据配置类型查询审计日志
     *
     * @param configType 配置类型
     * @param limit 限制条数
     * @return 审计日志列表
     */
    @Select("SELECT * FROM system_config_audit WHERE config_type = #{configType} ORDER BY operation_time DESC LIMIT #{limit}")
    List<SystemConfigAudit> findByConfigTypeOrderByOperationTimeDesc(@Param("configType") String configType, @Param("limit") int limit);

    /**
     * 根据操作人查询审计日志
     *
     * @param operator 操作人
     * @param limit 限制条数
     * @return 审计日志列表
     */
    @Select("SELECT * FROM system_config_audit WHERE operator = #{operator} ORDER BY operation_time DESC LIMIT #{limit}")
    List<SystemConfigAudit> findByOperatorOrderByOperationTimeDesc(@Param("operator") String operator, @Param("limit") int limit);

    /**
     * 根据时间范围查询审计日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制条数
     * @return 审计日志列表
     */
    @Select("SELECT * FROM system_config_audit WHERE operation_time BETWEEN #{startTime} AND #{endTime} ORDER BY operation_time DESC LIMIT #{limit}")
    List<SystemConfigAudit> findByOperationTimeBetweenOrderByOperationTimeDesc(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit);

    /**
     * 根据操作类型查询审计日志
     *
     * @param operationType 操作类型
     * @param limit 限制条数
     * @return 审计日志列表
     */
    @Select("SELECT * FROM system_config_audit WHERE operation_type = #{operationType} ORDER BY operation_time DESC LIMIT #{limit}")
    List<SystemConfigAudit> findByOperationTypeOrderByOperationTimeDesc(@Param("operationType") String operationType, @Param("limit") int limit);
}