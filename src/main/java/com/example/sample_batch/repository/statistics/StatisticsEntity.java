package com.example.sample_batch.repository.statistics;

import com.example.sample_batch.repository.booking.BookingEntity;
import com.example.sample_batch.repository.booking.BookingStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Entity
@Table(name = "statistics")
public class StatisticsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 생성을 DB에 위임합니다. (AUTO_INCREMENT)
    private Integer statisticsSeq;
    private LocalDateTime statisticsAt; // 일 단위

    private int allCount;
    private int attendedCount;
    private int cancelledCount;

    public static StatisticsEntity create(final BookingEntity bookingEntity) {
        StatisticsEntity statisticsEntity = new StatisticsEntity();
        statisticsEntity.setStatisticsAt(bookingEntity.getStatisticsAt());
        statisticsEntity.setAllCount(1);
        if (bookingEntity.isAttended()) {
            statisticsEntity.setAttendedCount(1);

        }
        if (BookingStatus.CANCELLED.equals(bookingEntity.getStatus())) {
            statisticsEntity.setCancelledCount(1);

        }
        return statisticsEntity;

    }

    public void add(final BookingEntity bookingEntity) {
        this.allCount++;

        if (bookingEntity.isAttended()) {
            this.attendedCount++;

        }
        if (BookingStatus.CANCELLED.equals(bookingEntity.getStatus())) {
            this.cancelledCount++;

        }

    }

}