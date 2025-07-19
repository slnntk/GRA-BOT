    package com.gra.paradise.botattendance.model;

    import jakarta.persistence.*;
    import lombok.Data;
    import org.hibernate.annotations.Fetch;
    import org.hibernate.annotations.FetchMode;

    import java.time.Instant;
    import java.util.ArrayList;
    import java.util.List;

    @Data
    @Entity
    @Table(name = "schedules")
    public class Schedule {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String guildId;
        private String title;
        private String createdById;
        private String createdByUsername;
        private Instant startTime;
        private Instant endTime;
        private boolean active = true;

        @Enumerated(EnumType.STRING)
        private AircraftType aircraftType;

        @Enumerated(EnumType.STRING)
        private MissionType missionType;

        @Enumerated(EnumType.STRING)
        @Column(nullable = true)
        private ActionSubType actionSubType;

        @Column(nullable = true)
        private String actionOption;

        @Column(nullable = true)
        private String outrosDescription;

        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(
                name = "schedule_crew",
                joinColumns = @JoinColumn(name = "schedule_id"),
                inverseJoinColumns = @JoinColumn(name = "user_discord_id")
        )
        @Fetch(FetchMode.SUBSELECT)
        private List<User> crewMembers = new ArrayList<>();

        @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        @Fetch(FetchMode.SUBSELECT)
        private List<ScheduleLog> logs = new ArrayList<>();

        private String messageId;
        private String channelId;

        private transient int crewMembersCount = -1;
        private transient List<User> initializedCrewMembers = null;

        public void addCrewMember(User user) {
            if (crewMembers == null) {
                crewMembers = new ArrayList<>();
            }
            crewMembers.add(user);
            user.getSchedules().add(this);
            initializeCrewMembers(); // Update transient fields after modification
        }

        public void removeCrewMember(User user) {
            if (crewMembers != null) {
                crewMembers.remove(user);
                user.getSchedules().remove(this);
                initializeCrewMembers(); // Update transient fields after modification
            }
        }

        public void initializeCrewMembers() {
            if (crewMembers != null) {
                this.crewMembersCount = crewMembers.size();
                this.initializedCrewMembers = new ArrayList<>(crewMembers);
            } else {
                this.crewMembersCount = 0;
                this.initializedCrewMembers = new ArrayList<>();
            }
        }

        public int getCrewMembersCount() {
            return crewMembersCount >= 0 ? crewMembersCount : 0;
        }

        public List<User> getInitializedCrewMembers() {
            return initializedCrewMembers != null ? new ArrayList<>(initializedCrewMembers) : new ArrayList<>();
        }

        @Override
        public String toString() {
            return "Schedule{" +
                    "id=" + id +
                    ", guildId='" + guildId + '\'' +
                    ", title='" + title + '\'' +
                    ", createdById='" + createdById + '\'' +
                    ", createdByUsername='" + createdByUsername + '\'' +
                    ", startTime=" + (startTime != null ? startTime.toString() : "null") +
                    ", endTime=" + (endTime != null ? endTime.toString() : "null") +
                    ", active=" + active +
                    ", aircraftType=" + aircraftType +
                    ", missionType=" + missionType +
                    ", actionSubType=" + actionSubType +
                    ", actionOption='" + actionOption + '\'' +
                    ", outrosDescription='" + outrosDescription + '\'' +
                    ", messageId='" + messageId + '\'' +
                    ", channelId='" + channelId + '\'' +
                    '}';
        }
    }