package com.flubburr.aioa.platform;

import com.flubburr.aioa.AioaConstants;
import com.flubburr.aioa.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> serviceClass) {
        T loadedService = ServiceLoader.load(serviceClass)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to load service for " + serviceClass.getName()));

        AioaConstants.LOG.debug("Loaded platform service {}", loadedService.getClass().getName());
        return loadedService;
    }
}
