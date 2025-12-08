package com.hotel.dto.review;

import lombok.Data;

import java.util.Map;

@Data
public class ReviewStatisticsResponse {

    private Long hotelId;

    private Long totalReviews;

    private Double overallRating;

    private Double cleanlinessRating;

    private Double serviceRating;

    private Double facilitiesRating;

    private Double locationRating;

    private RatingDistribution ratingDistribution;

    private Long reviewsWithImages;

    private Double averageCommentLength;

    @Data
    public static class RatingDistribution {
        private Long rating5;
        private Long rating4;
        private Long rating3;
        private Long rating2;
        private Long rating1;

        public RatingDistribution() {}

        public RatingDistribution(Long rating5, Long rating4, Long rating3, Long rating2, Long rating1) {
            this.rating5 = rating5;
            this.rating4 = rating4;
            this.rating3 = rating3;
            this.rating2 = rating2;
            this.rating1 = rating1;
        }

        public Long getTotal() {
            return rating5 + rating4 + rating3 + rating2 + rating1;
        }

        public Double getPercentage(Integer rating) {
            Long total = getTotal();
            if (total == 0) return 0.0;

            Long count = 0L;
            switch (rating) {
                case 5: count = rating5; break;
                case 4: count = rating4; break;
                case 3: count = rating3; break;
                case 2: count = rating2; break;
                case 1: count = rating1; break;
            }

            return (double) count / total * 100;
        }
    }
}