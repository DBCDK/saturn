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
import java.util.Objects;

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

    private Integer seqno;
    private String seqnoExtract;

    private String agency;

    private Boolean enabled;

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

    public Integer getSeqno() {
        return seqno;
    }

    public void setSeqno(Integer seqno) {
        this.seqno = seqno;
    }

    public String getSeqnoExtract() {
        return seqnoExtract;
    }

    public void setSeqnoExtract(String seqnoExtract) {
        this.seqnoExtract = seqnoExtract;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public Boolean isEnabled() {
        return enabled == null ? false : enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractHarvesterConfigEntity that = (AbstractHarvesterConfigEntity) o;
        return id == that.id &&
                Objects.equals(name, that.name) &&
                Objects.equals(schedule, that.schedule) &&
                Objects.equals(lastHarvested, that.lastHarvested) &&
                Objects.equals(transfile, that.transfile) &&
                Objects.equals(seqno, that.seqno) &&
                Objects.equals(seqnoExtract, that.seqnoExtract) &&
                Objects.equals(agency, that.agency) &&
                Objects.equals(enabled, that.enabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, schedule, lastHarvested,
                transfile, seqno, seqnoExtract, agency, enabled);
    }
}
