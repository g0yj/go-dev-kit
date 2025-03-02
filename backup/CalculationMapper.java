package com.app.api.common.mybatis;

import com.app.api.admin.controller.dto.ListCalculatesResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

@Mapper
public interface CalculationMapper {

  /**
   * 기간별 정산목록 요약
   */
  @SelectProvider(type = CalculateForPeriodProvider.class, method = "listCalculateSummaryForPeriod")
  List<ListCalculatesResponse.Summary> listCalculateSummaryForPeriod(
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("creatorName") String creatorName);

  /**
   * 기간별 정산목록 주문
   */
  @SelectProvider(type = CalculateForPeriodProvider.class, method = "listCalculateOrderForPeriod")
  List<ListCalculatesResponse.Order> listCalculatesOrderForPeriod(
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("creatorName") String creatorName);

  /**
   * 기간별 정산목록 결제
   */
  @SelectProvider(type = CalculateForPeriodProvider.class, method = "listCalculatePaymentForPeriod")
  List<ListCalculatesResponse.Payment> listCalculatesPaymentForPeriod(
      @Param("dateFrom") String dateFrom,
      @Param("dateTo") String dateTo,
      @Param("creatorName") String creatorName);
}
