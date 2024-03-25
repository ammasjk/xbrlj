package io.datanapis.xbrl.analysis;

public record LabelPair(String axisLabel, String memberLabel) implements Pair<String, String> {
    @Override
    public String getFirst() {
        return axisLabel;
    }

    @Override
    public String getSecond() {
        return memberLabel;
    }
}
