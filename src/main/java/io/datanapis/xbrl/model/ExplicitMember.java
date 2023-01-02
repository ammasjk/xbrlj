/*
 * Copyright (C) 2020 Jayakumar Muthukumarasamy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datanapis.xbrl.model;

import org.jetbrains.annotations.NotNull;

public final class ExplicitMember implements Comparable<ExplicitMember> {
    /* For a sample, see Context */
    private final Concept dimension;
    private final Concept member;

    public ExplicitMember(Concept dimension, Concept member) {
        assert (dimension != null && member != null);
        this.dimension = dimension;
        this.member = member;
    }

    public Concept getDimension() {
        return dimension;
    }

    public Concept getMember() {
        return member;
    }

    @Override
    public String toString() {
        return "(" + member.getQualifiedName() + ", " + dimension.getQualifiedName() + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExplicitMember that = (ExplicitMember) o;

        if (!dimension.equals(that.dimension)) return false;
        return member.equals(that.member);
    }

    @Override
    public int hashCode() {
        int result = dimension.hashCode();
        result = 31 * result + member.hashCode();
        return result;
    }

    @Override
    public int compareTo(@NotNull ExplicitMember o) {
        int result = dimension.getQualifiedName().compareTo(o.getDimension().getQualifiedName());
        if (result != 0)
            return result;
        return member.getQualifiedName().compareTo(o.member.getQualifiedName());
    }
}
