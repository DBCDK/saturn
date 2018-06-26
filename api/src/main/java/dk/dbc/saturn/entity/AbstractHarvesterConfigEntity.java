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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@MappedSuperclass
public abstract class AbstractHarvesterConfigEntity {
    @Id
    @SequenceGenerator(
        name = "harvesterconfig_id_seq",
        sequenceName = "harvesterconfig_id_seq",
        allocationSize = 1)
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "harvesterconfig_id_seq")
    private int id;

    private String name;

    private String schedule;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "lastharvested")
    private Date lastHarvested;

    private String transfile;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public Date getLastHarvested() {
        return lastHarvested;
    }

    public void setLastHarvested(Date lastHarvested) {
        this.lastHarvested = lastHarvested;
    }

    public String getTransfile() {
        return transfile;
    }

    public void setTransfile(String transfile) {
        this.transfile = transfile;
    }

    @SuppressWarnings("PMD.UselessParentheses")
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HttpHarvesterConfig)) return false;

        AbstractHarvesterConfigEntity that = (AbstractHarvesterConfigEntity) o;

        final boolean lastHarvestedEquals = (lastHarvested == null &&
            that.lastHarvested == null) || (lastHarvested != null &&
            lastHarvested.equals(that.lastHarvested));
        return id == that.id
            && name.equals(that.name)
            && schedule.equals(that.schedule)
            && lastHarvestedEquals
            && transfile.equals(that.transfile);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + schedule.hashCode();
        if(lastHarvested != null) {
            result = 31 * result + lastHarvested.hashCode();
        }
        result = 31 * result + transfile.hashCode();
        return result;
    }
}
