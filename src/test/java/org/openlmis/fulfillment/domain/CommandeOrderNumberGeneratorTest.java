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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openlmis.fulfillment.repository.OrderNumberSequenceRepository;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.ProgramReferenceDataService;
import org.openlmis.fulfillment.util.DateHelper;

@RunWith(MockitoJUnitRunner.class)
public class CommandeOrderNumberGeneratorTest {

  private static final String FACILITY_CODE = "HC01";
  private static final String PROGRAM_CODE = "VAR";
  private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

  @InjectMocks
  private CommandeOrderNumberGenerator generator;

  @Mock
  private OrderNumberSequenceRepository orderNumberSequenceRepository;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private DateHelper dateHelper;

  private final UUID programId = UUID.randomUUID();
  private final UUID supplyingFacilityId = UUID.randomUUID();
  private Order order;

  @Before
  public void setUp() {
    ProgramDto program = new ProgramDto();
    program.setId(programId);
    program.setCode(PROGRAM_CODE);

    FacilityDto facility = new FacilityDto();
    facility.setId(supplyingFacilityId);
    facility.setCode(FACILITY_CODE);

    order = new Order();
    order.setProgramId(programId);
    order.setSupplyingFacilityId(supplyingFacilityId);

    when(facilityReferenceDataService.findOne(supplyingFacilityId)).thenReturn(facility);
    when(programReferenceDataService.findOne(programId)).thenReturn(program);
    when(dateHelper.getCurrentDate()).thenReturn(TODAY);
  }

  @Test
  public void shouldGenerateOrderNumberWithDateFacilityCodeProgramCodeAndPaddedSequence() {
    when(orderNumberSequenceRepository.getNextSequenceValue(supplyingFacilityId, programId,
        2026, 9)).thenReturn(1);

    assertEquals("COMMANDE-28092026-HC01-VAR-0001", generator.generate(order));
  }

  @Test
  public void shouldUseNextSequenceValue() {
    when(orderNumberSequenceRepository.getNextSequenceValue(supplyingFacilityId, programId,
        2026, 9)).thenReturn(42);

    assertEquals("COMMANDE-28092026-HC01-VAR-0042", generator.generate(order));
  }

  @Test
  public void shouldNotTruncateSequenceAboveFourDigits() {
    when(orderNumberSequenceRepository.getNextSequenceValue(supplyingFacilityId, programId,
        2026, 9)).thenReturn(10000);

    assertEquals("COMMANDE-28092026-HC01-VAR-10000", generator.generate(order));
  }

  @Test
  public void shouldUseCurrentYearAndMonthAsSequenceKey() {
    when(dateHelper.getCurrentDate()).thenReturn(LocalDate.of(2027, 1, 5));
    when(orderNumberSequenceRepository.getNextSequenceValue(supplyingFacilityId, programId,
        2027, 1)).thenReturn(1);

    assertEquals("COMMANDE-05012027-HC01-VAR-0001", generator.generate(order));
  }

  @Test(expected = OrderNumberException.class)
  public void shouldThrowExceptionWhenProgramNotFound() {
    when(programReferenceDataService.findOne(programId)).thenReturn(null);

    try {
      generator.generate(order);
    } finally {
      verify(orderNumberSequenceRepository, never())
          .getNextSequenceValue(any(), any(), anyInt(), anyInt());
    }
  }

  @Test(expected = OrderNumberException.class)
  public void shouldThrowExceptionWhenFacilityNotFound() {
    when(facilityReferenceDataService.findOne(supplyingFacilityId)).thenReturn(null);

    try {
      generator.generate(order);
    } finally {
      verify(orderNumberSequenceRepository, never())
          .getNextSequenceValue(any(), any(), anyInt(), anyInt());
    }
  }

  @Test(expected = OrderNumberException.class)
  public void shouldThrowExceptionWhenSupplyingFacilityIsMissing() {
    order.setSupplyingFacilityId(null);

    generator.generate(order);
  }
}
