-- user_
insert into lms.user_ (id, login_id, name, name_en, last_name_en, password, type, gender, phone,
                       phone_type, cell_phone, is_receive_sms, email, is_receive_email, zipcode,
                       address, detailed_address, address_type, is_office_worker, company, position,
                       join_path, language, etc_language, language_skill, is_active,
                       foreign_country, foreign_period, foreign_purpose, course_purpose,
                       withdrawal_reason, note, nickname, textbook, created_by, created_on,
                       modified_by, modified_on)
values ('indian', 'indian', '전혜원', 'Julia, ', null, 'fc24b8bd46432ac10f2db576b88330a8', 'S', 'F',
        null, 'H', 'gDOsdgG4rTZudlvwhyyqOQ==', 'Y', 'indian_1026@hotmail.com', 'Y', '135-280',
        '서울 강남구 대치동', '889-13 금강타워 13층 멜라트은행', 'H', 'N', null, null, '900', null, null,
        'SJPT:,HKC:,TOEIC:,TSC:,기타:,TOEIC-S:,OPIc:', 0, null, null, null, '40,50', null, null, null,
        null, null, '20030718000000', null, '20030718000000');

-- teacher
-- M1698204875465368,LT,,20231201,SP_16,,,,,iz90+10owf/sbloMNjrwDg==,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,0,0,0,90,20231025123435,1,ENGLISH,1
insert into teacher (user_id, type, partner_teacher_id, work_start_date, work_time, work_type,
                     sms_type, home_address, home_phone, cell_phone, cell_phone_device_number,
                     cell_phone_lease_start_date, cell_phone_lease_end_date,
                     cell_phone_device_price, cell_phone_device_statue, university, major,
                     previous_work, nationality, visa_type, visa_entry_type, visa_end_date,
                     arrival_date, arrival_note, stay_start_date, stay_end_date, re_entry_note,
                     sponsor_start_date, sponsor_end_date, training_day, visa_note,
                     contract_start_date, contract_end_date, contract_basic_salary, contract_note,
                     pnp_code, airfare_payment, deposit, monthly, hire_type, landlord, zipcode,
                     address, detailed_address, phone, hire_note, hire_start_date, hire_end_date,
                     education_office, recruitment_date, education_office_note,
                     education_office_manager, contract_expiration_date, dismissal_immi_date,
                     departure_date, dismissal_manager, dismissal_me_date, release_note,
                     dismissal_note, basic_salary, housing_cost, management_cost, national_pension,
                     health_insurance, care_insurance, employment_insurance, sort, is_active,
                     language, created_by, created_on, modified_by, modified_on)
values ('M1698204875465368', 'LT', null, '20231201', 'SP_16', null, null, null, null,
        'iz90+10owf/sbloMNjrwDg==', null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, 0, 0, 0, 0, 0, 0, 0, 90, 1, 'ENGLISH',
        null, '20231025123435', null, '20231025123435');

-- schedule
insert into schedule (id, teacher_id, date, start_time, work_time, created_by, created_on,
                      modified_by, modified_on)
values (9525229, 'M1661138591404520', '20231229', '172000', 'PM_16', 'U1670565899112697',
        '20231229152941', 'U1670565899112697', '20231229152941');

-- reservation
insert into reservation (id, course_id, user_id, teacher_id, product_id, date, start_time, end_time,
                         attendance_status, report, created_by, created_on, modified_by,
                         modified_on)
values (2626298, 75125, 'M1699681314379069', 'M1698204875465368', null, '20231230', '170000',
        '173000', 'N', null, 'U1689811896976182', '20231230140855', 'U1689811896976182',
        '20231230140855');

-- refund
insert into refund (id, user_id, order_product_id, refund_date, refund_amount, cash_amount,
                    deposit_amount, bank, account_number, card_amount, refund_reason, created_by,
                    created_on, modified_by, modified_on)
values ('R9853186712751', 'M1496146897053847', 'I1496146913507964', '20170705', 880000, 880000, 0,
        null, null, 0, '프로그램 불만족', 'U1476844902135538', '20170705140630', 'U1476844902135538',
        '20170705140630');

-- payment
INSERT INTO lms.payment (id, user_id, order_id, payment_date, type, payment_method, payment_amount,
                         account_holder, account_number, card_company, card_number,
                         installment_months, approval_number, cancel_amount, cancel_date,
                         cancel_manager, memo, is_receipt_issued, receipt_number, deposit_amount,
                         transaction_name, company_number, account_transaction_date, created_by,
                         created_on, modified_by, modified_on)
VALUES ('PB102623604569140', 'M1568605720894582', 'O1616389685576893', '2021-03-22', 'I', 'BANK',
        2930000, '박병서', null, null, null, null, null, 0, null, null, null, 'N', null, null, null,
        null, null, 'U1600153561298058', '2021-03-22 14:15:48', 'U1600153561298058',
        '2021-03-22 14:15:48');

INSERT INTO lms.payment (id, user_id, order_id, payment_date, type, payment_method, payment_amount,
                         account_holder, account_number, card_company, card_number,
                         installment_months, approval_number, cancel_amount, cancel_date,
                         cancel_manager, memo, is_receipt_issued, receipt_number, deposit_amount,
                         transaction_name, company_number, account_transaction_date, created_by,
                         created_on, modified_by, modified_on)
VALUES ('PC11692193530351', 'M1487768940360315', 'O1499341662110813', '2017-07-26', 'I', 'CASH',
        340000, null, null, null, null, null, null, 0, null, null, null, 'Y', '010-9936-6671', null,
        null, null, null, 'U1476844902135538', '2017-07-26 20:56:37', 'U1476844902135538',
        '2017-07-26 20:56:37');

-- order
insert into order_ (id, type, user_id, supply_amount, discount_amount, billing_amount, cash_amount,
                    deposit_amount, card_count, card_amount, payment_amount, receivable_amount,
                    refund_amount, recall_date, receivable_reason, created_by, created_on,
                    modified_by, modified_on)
values ('O1703914356222821', 'F', 'M1672401688362469', 2970000, 40000, 2930000, 0, 0, 1, 2930000,
        2930000, 0, 0, null, null, 'U1689811896976182', '20231230143236', 'U1689811896976182',
        '20231230143236');

-- order_product
insert into order_product(id, order_id, product_id, teacher_id, months, quantity, is_retake,
                          retake_teacher_id, amount, discount_amount, payment_amount, note,
                          created_by, created_on, modified_by, modified_on)
values ('I1370429075063314', 'O1370429075058010', 'P1350556710397836', '501433', 2, 8, 'N', null,
        440000, 0, 440000, '구로 - 박근령', 'U1370416121434045', '20130605194435', 'U1370416121434045',
        '20130605194435');

-- member_consultation
insert into member_consultation (id, consultation_date, user_id, type, details, created_by,
                                 created_on, modified_by, modified_on)
values (296724, '20231230140000', 'M1672401688362469', '70',
        '재등록 결제완료  [재등록조건] 293M/6개월+6개월/48+2회 ', 'U1684455278686108', '20231230143345',
        'U1684455278686108', '20231230143345');

-- ldf
insert into ldf (id, user_id, reservation_id, lesson, content_sp, content_v, content_sg, content_c,
                 grade, evaluation, email_id, created_by, created_on, modified_by, modified_on)
values (368395, 'M1699681314379069', 2626297, '.', null, null, null, null, null, null, 354975,
        'M1657064932771055', '20231230174214', 'M1657064932771055', '20231230174214');

-- course
insert into course (id, user_id, order_product_id, teacher_id, assistant_teacher_id, lesson_count,
                    assignment_count, attendance_count, start_date, end_date, is_completion,
                    is_reservation, count_change_reason, date_change_reason, created_by, created_on,
                    modified_by, modified_on)
values (75615, 'M1672401688362469', 'I1703914356228679', 'M1695013438115743', 'M1681990831501448',
        50, 0, 0, '20231230', '20241229', 'N', 'N', '수강권추가구매', '어플 사용을 위해 수강시작일 임시변경',
        'U1689811896976182', '20231230143236', 'U1689811896976182', '20231230143553');

-- course_history
insert into course_history (id, course_id, module_id, module, type, title, content, created_by,
                            created_on, modified_by, modified_on)
values (3817532, 75587, '75587', 'COURSE_USER', '배정취소', null, '총 배정수 : 6회(web)',
        'M1703814296618972', '20231230213447', 'M1703814296618972', '20231230213447');

insert into course_history (id, course_id, module_id, module, type, title, content, created_by,
                            created_on, modified_by, modified_on)
values (3817365, NULL, '26268', 'phone', null, null,
        '[상담내용]  *현재 레벨 500 by Steven.T -강점: reading, listening -약점: speaking  *직장/자택 직장 : x 자택 : 동작구 신대방동  *상담계기  직장을 그만두고 쉬고 있으며 나중에 취업을 위해 회화를 하고싶어 함.   *기본내용   우선 당학원 연혁과 4가지 장점(3차원코칭, 컨텐츠-교재, 자유로운 예약, 무료그룹수업)에 대해 설명드리고 핸드폰으로 어플리케이션 시연(예약, CUBE숙제, LDF)을 보여드려 체계성 어필. 높은수준의 강사님의 능력과 질에 대해 어필함.  임상병리사로 일하다 지금 쉬고 있으며 하고싶은게 많은데 그 중에서도 영어는 꼭 필요할 것 같아 공부를 결심하셨다고 함. 토익이 920점 대로 문법 관련하여 공부는 많이 했고 필리핀으로 1개월 다녀온게 다이며 회화 공부는 따로 한 적이 없다고 함. 레벨테스트결과 레벨 500 정도로 듣기도 좋고 대화하는데 문제 없었다고 하며 스피킹 위주 수업으로 진행하면 괜찮을 것 같다고 함.  원어민 선생님 위주로 수업을 원하시나 한달에 한 두번씩 30분 이라도 한인선생님과도 한 번씩 문법을 잡으면서 수업하시는 걸 추천 드렸음, 수강료 안내 도와드리니 금액대가 생각보다 높다며 조금 더 생각해보고 연락주시겠다고 함.   *수강신청서 x  *교재 TTT1 or JE1 trial  *유입경로 인터넷 검색 : 영어회화구디역  *선호시간대 평일 오전',
        'U1684455278686108', '20231230132542', 'U1684455278686108', '20231230132542');

-- consultation
insert into consultation (id, institution_id, consultation_date, name, gender, job, company, phone,
                          cell_phone, found_path, found_path_note, visit_date, details, is_member,
                          type, study_purpose, etc_study_purpose, call_time, email, status,
                          level_test_type, level_test_answer, level_test_correct_count, created_by,
                          created_on, modified_by, modified_on)
values (26268, 'C1352081511487906', null, '원지연:상담완료', 'F', null, null, null,
        'tK25o9+FsnfFS0fhM2vaEw==', '20', '영어회화구디역', '20231230113000',
        '[레테] 12/30(토) 11:30am Steven.T 원지연 010-2727-7972', 'N', 'N', '40', null, null,
        'dnjswldus9720@naver.com', '7', null, null, NULL, 'U1675406451584692', '20231229214808',
        'U1675406451584692', '20231229214808');