package com.example.sample_batch.repository.notification;

import com.example.sample_batch.repository.booking.BookingEntity;
import com.example.sample_batch.repository.user.UserEntity;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-20T21:02:16+0900",
    comments = "version: 1.5.2.Final, compiler: javac, environment: Java 21.0.6 (Homebrew)"
)
public class NotificationModelMapperImpl implements NotificationModelMapper {

    @Override
    public NotificationEntity toNotificationEntity(BookingEntity bookingEntity, NotificationEvent event) {
        if ( bookingEntity == null && event == null ) {
            return null;
        }

        NotificationEntity notificationEntity = new NotificationEntity();

        if ( bookingEntity != null ) {
            notificationEntity.setUuid( bookingEntityUserEntityUuid( bookingEntity ) );
            notificationEntity.setText( text( bookingEntity.getStartedAt() ) );
        }
        notificationEntity.setEvent( event );

        return notificationEntity;
    }

    private String bookingEntityUserEntityUuid(BookingEntity bookingEntity) {
        if ( bookingEntity == null ) {
            return null;
        }
        UserEntity userEntity = bookingEntity.getUserEntity();
        if ( userEntity == null ) {
            return null;
        }
        String uuid = userEntity.getUuid();
        if ( uuid == null ) {
            return null;
        }
        return uuid;
    }
}
