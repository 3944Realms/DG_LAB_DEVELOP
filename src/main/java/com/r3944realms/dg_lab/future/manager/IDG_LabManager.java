package com.r3944realms.dg_lab.future.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IDG_LabManager {
    Logger logger = LoggerFactory.getLogger(IDG_LabManager.class);
    void start();
    void stop();
}
