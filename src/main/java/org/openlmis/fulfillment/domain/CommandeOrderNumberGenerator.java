/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.fulfillment.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.openlmis.fulfillment.extension.point.OrderNumberGenerator;
import org.openlmis.fulfillment.repository.OrderNumberSequenceRepository;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.ProgramReferenceDataService;
import org.openlmis.fulfillment.util.DateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Generates order numbers in the {@code COMMANDE-yyyyMMdd-PROGRAM_CODE-SEQUENCE} format,
 * e.g. {@code COMMANDE-20260928-VAR-0001}. The sequence is kept per supplying facility and
 * program and starts again from 1 every month. The date is taken in the service time zone
 * ({@code TIME_ZONE_ID}).
 */
@Component(value = "CommandeOrderNumberGenerator")
public class CommandeOrderNumberGenerator implements OrderNumberGenerator {

  static final String PREFIX = "COMMANDE";
  private static final String SEPARATOR = "-";
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

  @Autowired
  private OrderNumberSequenceRepository orderNumberSequenceRepository;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private DateHelper dateHelper;

  @Override
  public String generate(Order order) {
    UUID supplyingFacilityId = order.getSupplyingFacilityId();
    if (supplyingFacilityId == null) {
      throw new OrderNumberException("Order must have a supplying facility");
    }

    ProgramDto program = findProgram(order.getProgramId());
    LocalDate today = dateHelper.getCurrentDate();

    int sequenceValue = orderNumberSequenceRepository.getNextSequenceValue(
        supplyingFacilityId, order.getProgramId(), today.getYear(), today.getMonthValue());

    return String.join(SEPARATOR, PREFIX, today.format(DATE_FORMAT), program.getCode(),
        String.format("%04d", sequenceValue));
  }

  private ProgramDto findProgram(UUID programId) {
    ProgramDto program = programReferenceDataService.findOne(programId);

    if (program == null) {
      throw new OrderNumberException("Program not found: " + programId);
    }
    return program;
  }
}
