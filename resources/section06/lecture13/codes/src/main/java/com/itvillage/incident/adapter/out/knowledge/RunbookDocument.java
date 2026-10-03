package com.itvillage.incident.adapter.out.knowledge;

import java.util.List;

public record RunbookDocument(String service,
                              List<RunbookEntry> entries) {
}
