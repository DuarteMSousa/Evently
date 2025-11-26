package org.example.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TicketInformation {

    private String eventName;

    private String eventDate;

    private String venueName;

    private String tier;
}
