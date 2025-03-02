-- 상담 회원 여부
UPDATE consultation SET is_member = 'Y' WHERE is_member = '0';
UPDATE consultation SET is_member = 'N' WHERE is_member = '1';

-- 학사보고서 작성여부
UPDATE reservation
SET report_yn = IF(
        report IS NOT NULL
            OR today_lesson IS NOT NULL
            OR next_lesson IS NOT NULL,
        1, 0);