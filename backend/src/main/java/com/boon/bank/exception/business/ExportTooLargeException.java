package com.boon.bank.exception.business;

import com.boon.bank.exception.ErrorCode;

public class ExportTooLargeException extends BusinessException {

    private final long rowCount;
    private final long maxRows;

    public ExportTooLargeException(long rowCount, long maxRows) {
        super(ErrorCode.EXPORT_TOO_LARGE,
                "Export would contain " + rowCount + " rows (max " + maxRows + ") — narrow filters");
        this.rowCount = rowCount;
        this.maxRows = maxRows;
    }

    public long getRowCount() { return rowCount; }
    public long getMaxRows() { return maxRows; }
}
