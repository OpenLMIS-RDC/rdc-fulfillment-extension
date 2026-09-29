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

package org.openlmis.fulfillment.repository;

import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OrderNumberSequenceRepository {

  private static final String UPSERT_SQL =
      "INSERT INTO fulfillment.order_number_sequence "
          + "(supplyingfacilityid, programid, year, month, lastsequencevalue) "
          + "VALUES (?1, ?2, ?3, ?4, 1) "
          + "ON CONFLICT (supplyingfacilityid, programid, year, month) "
          + "DO UPDATE SET lastsequencevalue = order_number_sequence.lastsequencevalue + 1 "
          + "RETURNING lastsequencevalue";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Atomically inserts or increments the sequence value for a supplying facility, program and
   * month. The first call for a given key returns 1, subsequent calls increment it by 1.
   * Uses PostgreSQL's ON CONFLICT for row-level locking, so concurrent order creation never
   * receives the same value. The row stays locked until the order creating transaction ends,
   * so a rolled back order does not consume a number.
   *
   * @param supplyingFacilityId id of the order supplying facility
   * @param programId id of the order program
   * @param year year the sequence is kept for
   * @param month month (1-12) the sequence is kept for
   * @return next sequence value, starting from 1
   */
  @Transactional
  public int getNextSequenceValue(UUID supplyingFacilityId, UUID programId, int year,
      int month) {
    Query query = entityManager.createNativeQuery(UPSERT_SQL);
    query.setParameter(1, supplyingFacilityId);
    query.setParameter(2, programId);
    query.setParameter(3, year);
    query.setParameter(4, month);
    return ((Number) query.getSingleResult()).intValue();
  }
}
