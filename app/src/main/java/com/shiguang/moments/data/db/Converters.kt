package com.shiguang.moments.data.db

import androidx.room.TypeConverter
import com.shiguang.moments.data.models.MomentType
import com.shiguang.moments.data.models.Verdict

class Converters {
    @TypeConverter fun typeToString(v: MomentType) = v.name
    @TypeConverter fun stringToType(v: String) = MomentType.valueOf(v)

    @TypeConverter fun verdictToString(v: Verdict) = v.name
    @TypeConverter fun stringToVerdict(v: String) = Verdict.valueOf(v)
}