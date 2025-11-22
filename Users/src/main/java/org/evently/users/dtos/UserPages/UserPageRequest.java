package org.evently.users.dtos.UserPages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserPageRequest {

    private int pageSize;

    private int pageNumber;
}
