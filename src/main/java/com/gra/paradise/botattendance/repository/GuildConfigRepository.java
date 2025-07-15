package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.GuildConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildConfigRepository extends JpaRepository<GuildConfig, String> {
}