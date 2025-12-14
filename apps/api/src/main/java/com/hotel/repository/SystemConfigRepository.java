package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SystemConfigRepository extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查找配置
     */
    @Select("SELECT * FROM system_configs WHERE config_key = #{configKey}")
    Optional<SystemConfig> findByConfigKey(@Param("configKey") String configKey);

    /**
     * 根据配置类型查找所有配置
     */
    @Select("SELECT * FROM system_configs WHERE config_type = #{configType} ORDER BY config_key")
    List<SystemConfig> findByConfigType(@Param("configType") String configType);

    /**
     * 检查配置键是否存在
     */
    @Select("SELECT COUNT(1) FROM system_configs WHERE config_key = #{configKey}")
    boolean existsByConfigKey(@Param("configKey") String configKey);

    /**
     * 根据配置键和类型查找配置
     */
    @Select("SELECT * FROM system_configs WHERE config_key = #{configKey} AND config_type = #{configType}")
    Optional<SystemConfig> findByConfigKeyAndType(@Param("configKey") String configKey, @Param("configType") String configType);

    /**
     * 批量更新配置值
     */
    @Update("<script>" +
            "UPDATE system_configs SET config_value = #{configValue}, updated_at = NOW() WHERE config_key = #{configKey}" +
            "</script>")
    int updateConfigValue(@Param("configKey") String configKey, @Param("configValue") String configValue);

    /**
     * 根据类型删除所有配置
     */
    @Delete("DELETE FROM system_configs WHERE config_type = #{configType}")
    int deleteByConfigType(@Param("configType") String configType);
}