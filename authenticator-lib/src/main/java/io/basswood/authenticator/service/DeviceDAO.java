package io.basswood.authenticator.service;

import io.basswood.authenticator.exception.DuplicateEntityFound;
import io.basswood.authenticator.model.Device;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DeviceDAO {
    private Map<UUID, Device> repo;

    public DeviceDAO() {
        this.repo = new ConcurrentHashMap<>();
    }

    public Optional<Device> getDevice(UUID deviceId) {
        return Optional.ofNullable(repo.get(deviceId));
    }

    public Device create(Device device) {
        if (repo.containsKey(device.getDeviceId())) {
            throw new DuplicateEntityFound(Device.class, device.getDeviceId().toString());
        }
        repo.put(device.getDeviceId(), device);
        return device;
    }

    public Optional<Device> remove(UUID deviceId) {
        if (!repo.containsKey(deviceId)) {
            return Optional.empty();
        }
        Device device = repo.get(deviceId);
        repo.remove(deviceId);
        return Optional.of(device);
    }

    public Set<Device> removeByTag(String tag) {
        Set<Device> devices = repo.values().stream().filter(t -> t.getTags().contains(tag)).collect(Collectors.toSet());
        if (devices != null && !devices.isEmpty()) {
            devices.stream().forEach(t -> repo.remove(t.getDeviceId()));
        }
        return devices;
    }

    public void update(Device device) {
        repo.put(device.getDeviceId(), device);
    }

    public Set<Device> getDeviceByTag(String tag) {
        return repo.values().stream()
                .filter(t -> t.getTags() != null && !t.getTags().isEmpty() && t.getTags().contains(tag))
                .collect(Collectors.toSet());
    }

    public Set<Device> export() {
        return (repo == null || repo.isEmpty()) ?
                Collections.EMPTY_SET :
                this.repo.values().stream().collect(Collectors.toSet());
    }

    public Set<Device> clear() {
        Set<Device> removedItems = export();
        repo.clear();
        return removedItems;
    }
}
