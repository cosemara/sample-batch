package com.example.sample_batch.job.pass;

import com.example.sample_batch.repository.pass.BulkPassEntity;
import com.example.sample_batch.repository.pass.BulkPassRepository;
import com.example.sample_batch.repository.pass.BulkPassStatus;
import com.example.sample_batch.repository.pass.PassEntity;
import com.example.sample_batch.repository.pass.PassModelMapper;
import com.example.sample_batch.repository.pass.PassRepository;
import com.example.sample_batch.repository.user.UserGroupMappingEntity;
import com.example.sample_batch.repository.user.UserGroupMappingRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AddPassesTasklet implements Tasklet {
    private final PassRepository passRepository;
    private final BulkPassRepository bulkPassRepository;
    private final UserGroupMappingRepository userGroupMappingRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // 이용권 시작 일시 1일 전 user group 내 각 사용자에게 이용권을 추가해줍니다.
        final LocalDateTime startedAt = LocalDateTime.now().minusDays(1);
        final List<BulkPassEntity> bulkPassEntities = bulkPassRepository.findByStatusAndStartedAtGreaterThan(BulkPassStatus.READY, startedAt);

        int count = 0;
        for (BulkPassEntity bulkPassEntity : bulkPassEntities) {
            // user group에 속한 userId들을 조회합니다.
            final List<String> userIds = userGroupMappingRepository.findByUserGroupId(bulkPassEntity.getUserGroupId())
                                                                   .stream().map(UserGroupMappingEntity::getUserId).toList();

            // 각 userId로 이용권을 추가합니다.
            count += addPasses(bulkPassEntity, userIds);
            // pass 추가 이후 상태를 COMPLETED로 업데이트합니다.
            bulkPassEntity.setStatus(BulkPassStatus.COMPLETED);

        }
        log.info("AddPassesTasklet - execute: 이용권 {}건 추가 완료, startedAt={}", count, startedAt);
        return RepeatStatus.FINISHED;

    }

    // bulkPass의 정보로 pass 데이터를 생성합니다.
    private int addPasses(BulkPassEntity bulkPassEntity, List<String> userIds) {
        List<PassEntity> passEntities = new ArrayList<>();
        for (String userId : userIds) {
            PassEntity passEntity = PassModelMapper.INSTANCE.toPassEntity(bulkPassEntity, userId);
            passEntities.add(passEntity);

        }
        return passRepository.saveAll(passEntities).size();

    }

}