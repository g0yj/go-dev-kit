package com.app.api.database;

import com.app.api.jpa.entity.ConsultationEntity;
import com.app.api.jpa.repository.ConsultationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntityService {
    private final EntityManager entityManager;

    @Transactional
    public <T> void saveData(List<Map<String, String>> dataList, Class<T> entityClass, Map<String, String> fieldMappings) {
        if (dataList.isEmpty()) {
            log.warn("⚠️ [저장할 데이터 없음]");
            return;
        }


        List<T> entities = new ArrayList<>();
        for (Map<String, String> data : dataList) {
            T entity = mapToEntity(data, entityClass, fieldMappings);
            if (entity != null) {
                entities.add(entity);
            }
        }

        if (entities.isEmpty()) {
            log.warn("⚠️ [저장할 엔티티 없음] 모든 변환이 실패했거나 필터링됨.");
            return;
        }

        // ✅ JPA Batch Insert 최적화 (persist -> merge 또는 batch insert 고려)
        for (T entity : entities) {
            entityManager.persist(entity);
        }

        log.info("✅ [DB 저장 완료] 총 {}건 - 엔티티 타입: {}", entities.size(), entityClass.getSimpleName());
    }

    /**
     * 📌 CSV 데이터 → 엔티티 변환
     * @param data CSV에서 읽은 데이터 (Key-Value 형태)
     * @param entityClass 변환할 엔티티 클래스
     * @param fieldMappings CSV 헤더와 엔티티 필드 간의 매핑 정보
     * @return 변환된 엔티티 객체
     */
    public <T> T mapToEntity(Map<String, String> data, Class<T> entityClass, Map<String, String> fieldMappings) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, String> entry : data.entrySet()) {
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();

                // ✅ 필드 매핑 적용 (예: "이름" → "name")
                String entityFieldName = fieldMappings.getOrDefault(fieldName, fieldName);

                try {
                    Field field = entityClass.getDeclaredField(entityFieldName);
                    field.setAccessible(true);

                    // ✅ NULL 값 처리 및 기본값 설정
                    if (fieldValue == null || fieldValue.trim().isEmpty()) {
                        if (field.getType().equals(String.class)) {
                            field.set(entity, ""); // 문자열 필드의 기본값: 빈 문자열
                        } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                            field.set(entity, 0); // 숫자 필드의 기본값: 0
                        } else if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
                            field.set(entity, 0L);
                        } else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
                            field.set(entity, 0.0);
                        } else if (field.getType().equals(BigDecimal.class)) {
                            field.set(entity, BigDecimal.ZERO); // ✅ BigDecimal 기본값
                        }
                    } else {
                        // ✅ 값이 존재하면 변환 후 저장
                        if (field.getType().equals(String.class)) {
                            field.set(entity, fieldValue);
                        } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                            field.set(entity, Integer.parseInt(fieldValue));
                        } else if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
                            field.set(entity, Long.parseLong(fieldValue));
                        } else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
                            field.set(entity, Double.parseDouble(fieldValue));
                        } else if (field.getType().equals(BigDecimal.class)) {
                            field.set(entity, new BigDecimal(fieldValue));
                        }
                    }
                } catch (NoSuchFieldException e) {
                    log.warn("⚠️ [매핑된 필드 없음] 엔티티 '{}'에서 '{}' 필드가 존재하지 않음", entityClass.getSimpleName(), entityFieldName);
                }
            }

            return entity;
        } catch (Exception e) {
            log.error("❌ [엔티티 변환 실패] 데이터: {}", data, e);
            return null;
        }
    }

    /**
     * 📌 제네릭을 사용한 DB 저장 로직 (Batch Insert 가능)
     * @param dataList 변환된 데이터 리스트 (Key-Value 형태)
     * @param entityClass 저장할 엔티티 클래스
     * @param repository 엔티티에 해당하는 JpaRepository
     * @param <T> 엔티티 타입
     */
    @Transactional
    public <T> void saveAll(List<Map<String, String>> dataList, Class<T> entityClass, JpaRepository<T, ?> repository) {
        log.debug("💾 [DB 저장 시작] 엔티티: {}, 저장할 데이터 개수: {}", entityClass.getSimpleName(), dataList.size());

        for (Map<String, String> data : dataList) {
            try {
                T entity = entityClass.getDeclaredConstructor().newInstance();

                for (Map.Entry<String, String> entry : data.entrySet()) {
                    String fieldName = entry.getKey();
                    String value = entry.getValue();

                    entityClass.getDeclaredField(fieldName).setAccessible(true);
                    entityClass.getDeclaredField(fieldName).set(entity, value);
                }

                repository.save(entity);
                log.info("✅ 저장 완료: {}", entity);
            } catch (Exception e) {
                log.error("❌ [DB 저장 실패] 데이터: {}, 오류: {}", data, e.getMessage(), e);
            }
        }
    }

    /**
     * 📌 단일 엔티티 저장 (단건 Insert)
     */
    @Transactional
    public <T> void save(T entity, JpaRepository<T, ?> repository) {
        log.debug("💾 [단건 저장] 엔티티: {}", entity);
        repository.save(entity);
    }

    /**
     * 📌 ID 기준으로 엔티티 조회
     */
    public <T, ID> Optional<T> findById(ID id, JpaRepository<T, ID> repository) {
        log.debug("🔍 [ID 기준 조회] ID: {}", id);
        return repository.findById(id);
    }

    /**
     * 📌 모든 데이터 조회
     */
    public <T> List<T> findAll(JpaRepository<T, ?> repository) {
        log.debug("📌 [모든 데이터 조회]");
        return repository.findAll();
    }

    /**
     * 📌 엔티티 업데이트 (Key-Value 데이터 기반 업데이트)
     */
    @Transactional
    public <T, ID> void update(ID id, Map<String, String> updateData, Class<T> entityClass, JpaRepository<T, ID> repository) {
        Optional<T> optionalEntity = repository.findById(id);
        if (optionalEntity.isPresent()) {
            T entity = optionalEntity.get();
            try {
                for (Map.Entry<String, String> entry : updateData.entrySet()) {
                    String fieldName = entry.getKey();
                    String value = entry.getValue();

                    entityClass.getDeclaredField(fieldName).setAccessible(true);
                    entityClass.getDeclaredField(fieldName).set(entity, value);
                }

                repository.save(entity);
                log.debug("✅ [업데이트 완료] ID: {}, 데이터: {}", id, updateData);
            } catch (Exception e) {
                log.error("❌ [업데이트 실패] ID: {}, 오류: {}", id, e.getMessage(), e);
            }
        } else {
            log.warn("⚠️ [업데이트 실패] 존재하지 않는 ID: {}", id);
        }
    }

    /**
     * 📌 ID 기준으로 엔티티 삭제
     */
    @Transactional
    public <T, ID> void delete(ID id, JpaRepository<T, ID> repository) {
        log.debug("🗑 [삭제] ID: {}", id);
        repository.deleteById(id);
    }

    /**
     * 📌 모든 데이터 삭제
     */
    @Transactional
    public <T> void deleteAll(JpaRepository<T, ?> repository) {
        log.debug("🗑 [모든 데이터 삭제]");
        repository.deleteAll();
    }

    /**
     * 📌 batch 사용 시 반환 타입 문제 해결을 위함
     */
    @Transactional
    public void saveData(Chunk<? extends ConsultationEntity> items, JpaRepository<ConsultationEntity, Long> repository) {
        items.getItems().forEach(repository::save);
        log.info("✅ [DB 저장 완료] {}건", items.getItems().size());
    }
}
