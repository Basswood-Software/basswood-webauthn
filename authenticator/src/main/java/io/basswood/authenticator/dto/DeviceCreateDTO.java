package io.basswood.authenticator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCreateDTO {
    private UUID deviceId;
    private String displayName;
    private Set<String> tags;

    public UUID getDeviceId() {
        if(deviceId == null){
            setDeviceId(UUID.randomUUID());
        }
        return deviceId;
    }

    public String getDisplayName() {
        if(displayName == null){
            setDisplayName(String.format("Device-%s", getDeviceId()));
        }
        return displayName;
    }

    public Set<String> getTags() {
        if(tags == null){
            setTags(Set.of(ISO_LOCAL_DATE.format(LocalDate.now())));
        }
        return tags;
    }
}