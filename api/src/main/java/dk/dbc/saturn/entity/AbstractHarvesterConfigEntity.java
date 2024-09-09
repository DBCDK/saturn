/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.saturn.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dk.dbc.saturn.ProgressTrackerBean;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;

import java.util.Date;
import java.util.Objects;

@JsonIgnoreProperties(value={ "progress" }, allowGetters=true)
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

    private Boolean gzip;

    @Transient
    private ProgressTrackerBean.Progress progress;

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
        if (enabled == null) {
            return false;
        }
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getGzip() {
        if (gzip == null) {
            return false;
        }
        return gzip;
    }

    public void setGzip(Boolean gzip) {
        this.gzip = Objects.requireNonNullElse(gzip, false);
    }

    public ProgressTrackerBean.Progress getProgress() {
        return progress;
    }

    public AbstractHarvesterConfigEntity withProgress(ProgressTrackerBean.Progress progress) {
        this.progress = progress;
        return this;
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
                Objects.equals(enabled, that.enabled) &&
                Objects.equals(gzip, that.gzip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, schedule, lastHarvested,
                transfile, seqno, seqnoExtract, agency, enabled, gzip);
    }
}
