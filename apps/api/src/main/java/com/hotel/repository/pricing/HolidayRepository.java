package com.hotel.repository.pricing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.entity.pricing.Holiday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 节假日Repository
 */
@Mapper
public interface HolidayRepository extends BaseMapper<Holiday> {

    /**
     * 查找指定日期是否为节假日
     * @param date 日期
     * @return 节假日信息，可能为null
     */
    @Select("SELECT * FROM holidays " +
            "WHERE date = #{date} " +
            "AND deleted = 0 " +
            "LIMIT 1")
    Holiday findByDate(@Param("date") LocalDate date);

    /**
     * 查找指定年份的所有节假日
     * @param year 年份
     * @return 节假日列表
     */
    @Select("SELECT * FROM holidays " +
            "WHERE YEAR(date) = #{year} " +
            "AND deleted = 0 " +
            "ORDER BY date ASC")
    List<Holiday> findByYear(@Param("year") Integer year);

    /**
     * 查找指定日期范围内的节假日
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 节假日列表
     */
    @Select("SELECT * FROM holidays " +
            "WHERE date BETWEEN #{startDate} AND #{endDate} " +
            "AND deleted = 0 " +
            "ORDER BY date ASC")
    List<Holiday> findByDateRange(@Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * 查找指定年份的国家法定节假日
     * @param year 年份
     * @return 国家法定节假日列表
     */
    @Select("SELECT * FROM holidays " +
            "WHERE YEAR(date) = #{year} " +
            "AND is_national_holiday = true " +
            "AND deleted = 0 " +
            "ORDER BY date ASC")
    List<Holiday> findNationalHolidaysByYear(@Param("year") Integer year);

    /**
     * 查找未来的节假日
     * @param fromDate 起始日期
     * @return 未来的节假日列表
     */
    @Select("SELECT * FROM holidays " +
            "WHERE date >= #{fromDate} " +
            "AND deleted = 0 " +
            "ORDER BY date ASC " +
            "LIMIT 50")
    List<Holiday> findFutureHolidays(@Param("fromDate") LocalDate fromDate);

    /**
     * 检查指定日期是否为节假日
     * @param date 日期
     * @return 是否为节假日
     */
    @Select("SELECT COUNT(*) > 0 FROM holidays " +
            "WHERE date = #{date} " +
            "AND deleted = 0")
    boolean isHoliday(@Param("date") LocalDate date);

    /**
     * 检查指定日期是否为国家法定节假日
     * @param date 日期
     * @return 是否为国家法定节假日
     */
    @Select("SELECT COUNT(*) > 0 FROM holidays " +
            "WHERE date = #{date} " +
            "AND is_national_holiday = true " +
            "AND deleted = 0")
    boolean isNationalHoliday(@Param("date") LocalDate date);
}