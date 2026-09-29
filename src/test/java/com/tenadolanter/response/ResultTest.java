package com.tenadolanter.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void failCopiesResultCode() {
        Result<String> failed = Result.fail(CommonResultCode.NOT_FOUND);
        Result<String> ok = Result.ok("payload");

        assertThat(failed.getCode()).isEqualTo(CommonResultCode.NOT_FOUND.getCode());
        assertThat(failed.getMessage()).isEqualTo(CommonResultCode.NOT_FOUND.getMessage());
        assertThat(failed.getData()).isNull();
        assertThat(failed.isSuccess()).isEqualTo(failed.getCode() == ok.getCode());
    }

    @Test
    void failCanOverrideMessage() {
        Result<String> failed = Result.fail(CommonResultCode.NOT_FOUND, "missing");

        assertThat(failed.getCode()).isEqualTo(CommonResultCode.NOT_FOUND.getCode());
        assertThat(failed.getMessage()).isEqualTo("missing");
    }

    @Test
    void dataAndTimestampStayMutable() {
        Result<String> result = Result.ok("payload");
        long timestamp = result.getTimestamp();

        result.setData("next");
        result.setTimestamp(timestamp + 1);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("next");
        assertThat(result.getTimestamp()).isEqualTo(timestamp + 1);
    }
}
