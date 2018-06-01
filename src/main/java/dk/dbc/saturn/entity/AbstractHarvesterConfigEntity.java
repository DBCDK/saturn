/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.entity;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;
import java.sql.Timestamp;

@MappedSuperclass
abstract class AbstractHarvesterConfigEntity {
    @Id
    @SequenceGenerator(
        name = "harvesterconfig_id_seq",
        sequenceName = "harvesterconfig_id_seq",
        allocationSize = 1)
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "harvesterconfig_id_seq")
    private int id;

    private String schedule;

    @Column(name = "lastharvested")
    private Timestamp lastHarvested;

    public int getId() {
        return id;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public Timestamp getLastHarvested() {
        return lastHarvested;
    }

    public void setLastHarvested(Timestamp lastHarvested) {
        this.lastHarvested = lastHarvested;
    }
}
