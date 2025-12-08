package com.hotel.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.entity.WaitingList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优化的等待列表Repository
 * 提供高效的分页查询和统计功能
 *
 * @author System
 * @since 1.0
 */
@Mapper
public interface OptimizedWaitingListRepository extends BaseMapper<WaitingList> {

    /**
     * 高效分页查询用户等待列表
     * 使用索引优化，避免全表扫描
     */
    @Select("<script>" +
            "SELECT * FROM waiting_list WHERE deleted = 0 " +
            "AND user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if> " +
            "ORDER BY priority DESC, created_at ASC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    IPage<WaitingList> selectWaitingListPageOptimized(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("offset") Long offset,
            @Param("size") Integer size,
            Page<WaitingList> page);

    /**
     * 统计用户等待列表总数（带状态过滤）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM waiting_list WHERE deleted = 0 " +
            "AND user_id = #{userId} " +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</script>")
    Long countWaitingListByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") String status);

    /**
     * 高效查询房间等待列表
     * 按优先级和创建时间排序，支持分页
     */
    @Select("SELECT * FROM waiting_list WHERE deleted = 0 " +
            "AND room_id = #{roomId} " +
            "AND status = 'WAITING' " +
            "ORDER BY priority DESC, created_at ASC " +
            "LIMIT #{limit}")
    List<WaitingList> selectWaitingListByRoomIdWithLimit(@Param("roomId") Long roomId, @Param("limit") Integer limit);

    /**
     * 获取等待列表中的位置（优化版本）
     */
    @Select("SELECT COUNT(*) + 1 FROM waiting_list " +
            "WHERE deleted = 0 " +
            "AND room_id = #{roomId} " +
            "AND status = 'WAITING' " +
            "AND (priority > #{priority} OR (priority = #{priority} AND created_at < #{createdAt}))")
    Integer getWaitingListPositionOptimized(
            @Param("roomId") Long roomId,
            @Param("priority") Integer priority,
            @Param("createdAt") LocalDateTime createdAt);

    /**
     * 查询过期的等待列表（批量处理）
     */
    @Select("SELECT * FROM waiting_list WHERE deleted = 0 " +
            "AND status = 'NOTIFIED' " +
            "AND expires_at < #{now} " +
            "ORDER BY expires_at ASC " +
            "LIMIT #{batchSize}")
    List<WaitingList> selectExpiredWaitingListBatch(@Param("now") LocalDateTime now, @Param("batchSize") Integer batchSize);

    /**
     * 更新等待列表状态（批量）
     */
    @Select("<script>" +
            "UPDATE waiting_list SET " +
            "status = #{newStatus}, " +
            "updated_at = NOW() " +
            "WHERE deleted = 0 " +
            "AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateWaitingListStatus(@Param("ids") List<Long> ids, @Param("newStatus") String newStatus);

    /**
     * 查询特定时间范围内的等待列表统计
     */
    @Select("<script>" +
            "SELECT status, COUNT(*) as count FROM waiting_list " +
            "WHERE deleted = 0 " +
            "AND created_at BETWEEN #{startDate} AND #{endDate} " +
            "<if test='roomId != null'>" +
            "AND room_id = #{roomId} " +
            "</if> " +
            "GROUP BY status" +
            "</script>")
    List<WaitingListStatusCount> selectWaitingListStatsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("roomId") Long roomId);

    /**
     * 查询等待列表平均等待时间
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(HOUR, created_at, " +
            "CASE WHEN confirmed_at IS NOT NULL THEN confirmed_at ELSE NOW() END)) as avg_wait_hours " +
            "FROM waiting_list WHERE deleted = 0 " +
            "AND status IN ('CONFIRMED', 'EXPIRED') " +
            "<if test='roomId != null'>" +
            "AND room_id = #{roomId} " +
            "</if> " +
            "AND created_at BETWEEN #{startDate} AND #{endDate}")
    Double selectAverageWaitingTime(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("roomId") Long roomId);

    /**
     * 查询热门房间（等待列表数量最多的房间）
     */
    @Select("SELECT room_id, COUNT(*) as waiting_count " +
            "FROM waiting_list WHERE deleted = 0 " +
            "AND status = 'WAITING' " +
            "AND created_at >= #{sinceDate} " +
            "GROUP BY room_id " +
            "ORDER BY waiting_count DESC " +
            "LIMIT #{limit}")
    List<RoomWaitingCount> selectHotRooms(@Param("sinceDate") LocalDateTime sinceDate, @Param("limit") Integer limit);

    /**
     * 查询等待列表转化率统计
     */
    @Select("SELECT " +
            "COUNT(*) as total_waiting, " +
            "SUM(CASE WHEN status = 'CONFIRMED' THEN 1 ELSE 0 END) as confirmed_count, " +
            "SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) as expired_count " +
            "FROM waiting_list WHERE deleted = 0 " +
            "AND created_at BETWEEN #{startDate} AND #{endDate} " +
            "<if test='roomId != null'>" +
            "AND room_id = #{roomId} " +
            "</script>")
    WaitingListConversionStats selectConversionStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("roomId") Long roomId);

    /**
     * 清理过期等待列表（软删除）
     */
    @Select("UPDATE waiting_list SET " +
            "deleted = 1, " +
            "updated_at = NOW() " +
            "WHERE deleted = 0 " +
            "AND (status = 'EXPIRED' AND updated_at < #{cutoffDate}) " +
            "OR (status = 'NOTIFIED' AND expires_at < #{cutoffDate})")
    int cleanUpExpiredWaitingList(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 内部类：等待列表状态统计
     */
    class WaitingListStatusCount {
        private String status;
        private Long count;

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * 内部类：房间等待数量统计
     */
    class RoomWaitingCount {
        private Long roomId;
        private Long waitingCount;

        // Getters and setters
        public Long getRoomId() { return roomId; }
        public void setRoomId(Long roomId) { this.roomId = roomId; }
        public Long getWaitingCount() { return waitingCount; }
        public void setWaitingCount(Long waitingCount) { this.waitingCount = waitingCount; }
    }

    /**
     * 内部类：转化率统计
     */
    class WaitingListConversionStats {
        private Long totalWaiting;
        private Long confirmedCount;
        private Long expiredCount;

        // Getters and setters
        public Long getTotalWaiting() { return totalWaiting; }
        public void setTotalWaiting(Long totalWaiting) { this.totalWaiting = totalWaiting; }
        public Long getConfirmedCount() { return confirmedCount; }
        public void setConfirmedCount(Long confirmedCount) { this.confirmedCount = confirmedCount; }
        public Long getExpiredCount() { return expiredCount; }
        public void setExpiredCount(Long expiredCount) { this.expiredCount = expiredCount; }

        public Double getConversionRate() {
            if (totalWaiting == null || totalWaiting == 0) {
                return 0.0;
            }
            return (double) confirmedCount / totalWaiting * 100;
        }
    }
}