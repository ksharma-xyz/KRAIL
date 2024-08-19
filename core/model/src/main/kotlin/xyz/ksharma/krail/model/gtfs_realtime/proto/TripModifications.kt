// Code generated by Wire protocol buffer compiler, do not edit.
// Source: transit_realtime.TripModifications in xyz/ksharma/transport/gtfs_realtime.proto
@file:Suppress("DEPRECATION")

package xyz.ksharma.krail.model.gtfs_realtime.proto

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.JvmField
import com.squareup.wire.`internal`.JvmSynthetic
import com.squareup.wire.`internal`.checkElementsNotNull
import com.squareup.wire.`internal`.immutableCopyOf
import com.squareup.wire.`internal`.redactElements
import com.squareup.wire.`internal`.sanitize
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import okio.ByteString

/**
 * NOTE: This field is still experimental, and subject to change. It may be formally adopted in the
 * future.
 */
public class TripModifications(
  selected_trips: List<SelectedTrips> = emptyList(),
  start_times: List<String> = emptyList(),
  service_dates: List<String> = emptyList(),
  modifications: List<Modification> = emptyList(),
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<TripModifications, TripModifications.Builder>(ADAPTER, unknownFields) {
  /**
   * A list of selected trips affected by this TripModifications.
   */
  @field:WireField(
    tag = 1,
    adapter = "xyz.ksharma.krail.model.gtfs_realtime.proto.TripModifications${'$'}SelectedTrips#ADAPTER",
    label = WireField.Label.REPEATED,
    schemaIndex = 0,
  )
  @JvmField
  public val selected_trips: List<SelectedTrips> = immutableCopyOf("selected_trips", selected_trips)

  /**
   * A list of start times in the real-time trip descriptor for the trip_id defined in trip_ids.
   * Useful to target multiple departures of a trip_id in a frequency-based trip.
   */
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    label = WireField.Label.REPEATED,
    schemaIndex = 1,
  )
  @JvmField
  public val start_times: List<String> = immutableCopyOf("start_times", start_times)

  /**
   * Dates on which the modifications occurs, in the YYYYMMDD format. Producers SHOULD only transmit
   * detours occurring within the next week.
   * The dates provided should not be used as user-facing information, if a user-facing start and
   * end date needs to be provided, they can be provided in the linked service alert with
   * `service_alert_id`
   */
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    label = WireField.Label.REPEATED,
    schemaIndex = 2,
  )
  @JvmField
  public val service_dates: List<String> = immutableCopyOf("service_dates", service_dates)

  /**
   * A list of modifications to apply to the affected trips.
   */
  @field:WireField(
    tag = 4,
    adapter = "xyz.ksharma.krail.model.gtfs_realtime.proto.TripModifications${'$'}Modification#ADAPTER",
    label = WireField.Label.REPEATED,
    schemaIndex = 3,
  )
  @JvmField
  public val modifications: List<Modification> = immutableCopyOf("modifications", modifications)

  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.selected_trips = selected_trips
    builder.start_times = start_times
    builder.service_dates = service_dates
    builder.modifications = modifications
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is TripModifications) return false
    if (unknownFields != other.unknownFields) return false
    if (selected_trips != other.selected_trips) return false
    if (start_times != other.start_times) return false
    if (service_dates != other.service_dates) return false
    if (modifications != other.modifications) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + selected_trips.hashCode()
      result = result * 37 + start_times.hashCode()
      result = result * 37 + service_dates.hashCode()
      result = result * 37 + modifications.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (selected_trips.isNotEmpty()) result += """selected_trips=$selected_trips"""
    if (start_times.isNotEmpty()) result += """start_times=${sanitize(start_times)}"""
    if (service_dates.isNotEmpty()) result += """service_dates=${sanitize(service_dates)}"""
    if (modifications.isNotEmpty()) result += """modifications=$modifications"""
    return result.joinToString(prefix = "TripModifications{", separator = ", ", postfix = "}")
  }

  public fun copy(
    selected_trips: List<SelectedTrips> = this.selected_trips,
    start_times: List<String> = this.start_times,
    service_dates: List<String> = this.service_dates,
    modifications: List<Modification> = this.modifications,
    unknownFields: ByteString = this.unknownFields,
  ): TripModifications = TripModifications(selected_trips, start_times, service_dates,
      modifications, unknownFields)

  public class Builder : Message.Builder<TripModifications, Builder>() {
    @JvmField
    public var selected_trips: List<SelectedTrips> = emptyList()

    @JvmField
    public var start_times: List<String> = emptyList()

    @JvmField
    public var service_dates: List<String> = emptyList()

    @JvmField
    public var modifications: List<Modification> = emptyList()

    /**
     * A list of selected trips affected by this TripModifications.
     */
    public fun selected_trips(selected_trips: List<SelectedTrips>): Builder {
      checkElementsNotNull(selected_trips)
      this.selected_trips = selected_trips
      return this
    }

    /**
     * A list of start times in the real-time trip descriptor for the trip_id defined in trip_ids.
     * Useful to target multiple departures of a trip_id in a frequency-based trip.
     */
    public fun start_times(start_times: List<String>): Builder {
      checkElementsNotNull(start_times)
      this.start_times = start_times
      return this
    }

    /**
     * Dates on which the modifications occurs, in the YYYYMMDD format. Producers SHOULD only
     * transmit detours occurring within the next week.
     * The dates provided should not be used as user-facing information, if a user-facing start and
     * end date needs to be provided, they can be provided in the linked service alert with
     * `service_alert_id`
     */
    public fun service_dates(service_dates: List<String>): Builder {
      checkElementsNotNull(service_dates)
      this.service_dates = service_dates
      return this
    }

    /**
     * A list of modifications to apply to the affected trips.
     */
    public fun modifications(modifications: List<Modification>): Builder {
      checkElementsNotNull(modifications)
      this.modifications = modifications
      return this
    }

    override fun build(): TripModifications = TripModifications(
      selected_trips = selected_trips,
      start_times = start_times,
      service_dates = service_dates,
      modifications = modifications,
      unknownFields = buildUnknownFields()
    )
  }

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<TripModifications> = object : ProtoAdapter<TripModifications>(
      FieldEncoding.LENGTH_DELIMITED, 
      TripModifications::class, 
      "type.googleapis.com/transit_realtime.TripModifications", 
      PROTO_2, 
      null, 
      "xyz/ksharma/transport/gtfs_realtime.proto"
    ) {
      override fun encodedSize(`value`: TripModifications): Int {
        var size = value.unknownFields.size
        size += SelectedTrips.ADAPTER.asRepeated().encodedSizeWithTag(1, value.selected_trips)
        size += ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(2, value.start_times)
        size += ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(3, value.service_dates)
        size += Modification.ADAPTER.asRepeated().encodedSizeWithTag(4, value.modifications)
        return size
      }

      override fun encode(writer: ProtoWriter, `value`: TripModifications) {
        SelectedTrips.ADAPTER.asRepeated().encodeWithTag(writer, 1, value.selected_trips)
        ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 2, value.start_times)
        ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 3, value.service_dates)
        Modification.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.modifications)
        writer.writeBytes(value.unknownFields)
      }

      override fun encode(writer: ReverseProtoWriter, `value`: TripModifications) {
        writer.writeBytes(value.unknownFields)
        Modification.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.modifications)
        ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 3, value.service_dates)
        ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 2, value.start_times)
        SelectedTrips.ADAPTER.asRepeated().encodeWithTag(writer, 1, value.selected_trips)
      }

      override fun decode(reader: ProtoReader): TripModifications {
        val selected_trips = mutableListOf<SelectedTrips>()
        val start_times = mutableListOf<String>()
        val service_dates = mutableListOf<String>()
        val modifications = mutableListOf<Modification>()
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> selected_trips.add(SelectedTrips.ADAPTER.decode(reader))
            2 -> start_times.add(ProtoAdapter.STRING.decode(reader))
            3 -> service_dates.add(ProtoAdapter.STRING.decode(reader))
            4 -> modifications.add(Modification.ADAPTER.decode(reader))
            else -> reader.readUnknownField(tag)
          }
        }
        return TripModifications(
          selected_trips = selected_trips,
          start_times = start_times,
          service_dates = service_dates,
          modifications = modifications,
          unknownFields = unknownFields
        )
      }

      override fun redact(`value`: TripModifications): TripModifications = value.copy(
        selected_trips = value.selected_trips.redactElements(SelectedTrips.ADAPTER),
        modifications = value.modifications.redactElements(Modification.ADAPTER),
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L

    @JvmSynthetic
    public inline fun build(body: Builder.() -> Unit): TripModifications =
        Builder().apply(body).build()
  }

  /**
   * A `Modification` message replaces a span of n stop times from each affected trip starting at
   * `start_stop_selector`.
   */
  public class Modification(
    /**
     * The stop selector of the first stop_time of the original trip that is to be affected by this
     * modification.
     * Used in conjuction with `end_stop_selector`.
     * `start_stop_selector` is required and is used to define the reference stop used with
     * `travel_time_to_stop`.
     */
    @field:WireField(
      tag = 1,
      adapter = "xyz.ksharma.krail.model.gtfs_realtime.proto.StopSelector#ADAPTER",
      schemaIndex = 0,
    )
    @JvmField
    public val start_stop_selector: StopSelector? = null,
    /**
     * The stop selector of the last stop of the original trip that is to be affected by this
     * modification.
     * The selection is inclusive, so if only one stop_time is replaced by that modification,
     * `start_stop_selector` and `end_stop_selector` must be equivalent.
     * If no stop_time is replaced, `end_stop_selector` must not be provided. It's otherwise
     * required.
     */
    @field:WireField(
      tag = 2,
      adapter = "xyz.ksharma.krail.model.gtfs_realtime.proto.StopSelector#ADAPTER",
      schemaIndex = 1,
    )
    @JvmField
    public val end_stop_selector: StopSelector? = null,
    /**
     * The number of seconds of delay to add to all departure and arrival times following the end of
     * this modification.
     * If multiple modifications apply to the same trip, the delays accumulate as the trip advances.
     */
    @field:WireField(
      tag = 3,
      adapter = "com.squareup.wire.ProtoAdapter#INT32",
      schemaIndex = 2,
    )
    @JvmField
    public val propagated_modification_delay: Int? = null,
    replacement_stops: List<ReplacementStop> = emptyList(),
    /**
     * An `id` value from the `FeedEntity` message that contains the `Alert` describing this
     * Modification for user-facing communication.
     */
    @field:WireField(
      tag = 5,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      schemaIndex = 4,
    )
    @JvmField
    public val service_alert_id: String? = null,
    /**
     * This timestamp identifies the moment when the modification has last been changed.
     * In POSIX time (i.e., number of seconds since January 1st 1970 00:00:00 UTC).
     */
    @field:WireField(
      tag = 6,
      adapter = "com.squareup.wire.ProtoAdapter#UINT64",
      schemaIndex = 5,
    )
    @JvmField
    public val last_modified_time: Long? = null,
    unknownFields: ByteString = ByteString.EMPTY,
  ) : Message<Modification, Modification.Builder>(ADAPTER, unknownFields) {
    /**
     * A list of replacement stops, replacing those of the original trip.
     * The length of the new stop times may be less, the same, or greater than the number of
     * replaced stop times.
     */
    @field:WireField(
      tag = 4,
      adapter = "xyz.ksharma.krail.model.gtfs_realtime.proto.ReplacementStop#ADAPTER",
      label = WireField.Label.REPEATED,
      schemaIndex = 3,
    )
    @JvmField
    public val replacement_stops: List<ReplacementStop> = immutableCopyOf("replacement_stops",
        replacement_stops)

    override fun newBuilder(): Builder {
      val builder = Builder()
      builder.start_stop_selector = start_stop_selector
      builder.end_stop_selector = end_stop_selector
      builder.propagated_modification_delay = propagated_modification_delay
      builder.replacement_stops = replacement_stops
      builder.service_alert_id = service_alert_id
      builder.last_modified_time = last_modified_time
      builder.addUnknownFields(unknownFields)
      return builder
    }

    override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is Modification) return false
      if (unknownFields != other.unknownFields) return false
      if (start_stop_selector != other.start_stop_selector) return false
      if (end_stop_selector != other.end_stop_selector) return false
      if (propagated_modification_delay != other.propagated_modification_delay) return false
      if (replacement_stops != other.replacement_stops) return false
      if (service_alert_id != other.service_alert_id) return false
      if (last_modified_time != other.last_modified_time) return false
      return true
    }

    override fun hashCode(): Int {
      var result = super.hashCode
      if (result == 0) {
        result = unknownFields.hashCode()
        result = result * 37 + (start_stop_selector?.hashCode() ?: 0)
        result = result * 37 + (end_stop_selector?.hashCode() ?: 0)
        result = result * 37 + (propagated_modification_delay?.hashCode() ?: 0)
        result = result * 37 + replacement_stops.hashCode()
        result = result * 37 + (service_alert_id?.hashCode() ?: 0)
        result = result * 37 + (last_modified_time?.hashCode() ?: 0)
        super.hashCode = result
      }
      return result
    }

    override fun toString(): String {
      val result = mutableListOf<String>()
      if (start_stop_selector != null) result += """start_stop_selector=$start_stop_selector"""
      if (end_stop_selector != null) result += """end_stop_selector=$end_stop_selector"""
      if (propagated_modification_delay != null) result +=
          """propagated_modification_delay=$propagated_modification_delay"""
      if (replacement_stops.isNotEmpty()) result += """replacement_stops=$replacement_stops"""
      if (service_alert_id != null) result += """service_alert_id=${sanitize(service_alert_id)}"""
      if (last_modified_time != null) result += """last_modified_time=$last_modified_time"""
      return result.joinToString(prefix = "Modification{", separator = ", ", postfix = "}")
    }

    public fun copy(
      start_stop_selector: StopSelector? = this.start_stop_selector,
      end_stop_selector: StopSelector? = this.end_stop_selector,
      propagated_modification_delay: Int? = this.propagated_modification_delay,
      replacement_stops: List<ReplacementStop> = this.replacement_stops,
      service_alert_id: String? = this.service_alert_id,
      last_modified_time: Long? = this.last_modified_time,
      unknownFields: ByteString = this.unknownFields,
    ): Modification = Modification(start_stop_selector, end_stop_selector,
        propagated_modification_delay, replacement_stops, service_alert_id, last_modified_time,
        unknownFields)

    public class Builder : Message.Builder<Modification, Builder>() {
      @JvmField
      public var start_stop_selector: StopSelector? = null

      @JvmField
      public var end_stop_selector: StopSelector? = null

      @JvmField
      public var propagated_modification_delay: Int? = null

      @JvmField
      public var replacement_stops: List<ReplacementStop> = emptyList()

      @JvmField
      public var service_alert_id: String? = null

      @JvmField
      public var last_modified_time: Long? = null

      /**
       * The stop selector of the first stop_time of the original trip that is to be affected by
       * this modification.
       * Used in conjuction with `end_stop_selector`.
       * `start_stop_selector` is required and is used to define the reference stop used with
       * `travel_time_to_stop`.
       */
      public fun start_stop_selector(start_stop_selector: StopSelector?): Builder {
        this.start_stop_selector = start_stop_selector
        return this
      }

      /**
       * The stop selector of the last stop of the original trip that is to be affected by this
       * modification.
       * The selection is inclusive, so if only one stop_time is replaced by that modification,
       * `start_stop_selector` and `end_stop_selector` must be equivalent.
       * If no stop_time is replaced, `end_stop_selector` must not be provided. It's otherwise
       * required.
       */
      public fun end_stop_selector(end_stop_selector: StopSelector?): Builder {
        this.end_stop_selector = end_stop_selector
        return this
      }

      /**
       * The number of seconds of delay to add to all departure and arrival times following the end
       * of this modification.
       * If multiple modifications apply to the same trip, the delays accumulate as the trip
       * advances.
       */
      public fun propagated_modification_delay(propagated_modification_delay: Int?): Builder {
        this.propagated_modification_delay = propagated_modification_delay
        return this
      }

      /**
       * A list of replacement stops, replacing those of the original trip.
       * The length of the new stop times may be less, the same, or greater than the number of
       * replaced stop times.
       */
      public fun replacement_stops(replacement_stops: List<ReplacementStop>): Builder {
        checkElementsNotNull(replacement_stops)
        this.replacement_stops = replacement_stops
        return this
      }

      /**
       * An `id` value from the `FeedEntity` message that contains the `Alert` describing this
       * Modification for user-facing communication.
       */
      public fun service_alert_id(service_alert_id: String?): Builder {
        this.service_alert_id = service_alert_id
        return this
      }

      /**
       * This timestamp identifies the moment when the modification has last been changed.
       * In POSIX time (i.e., number of seconds since January 1st 1970 00:00:00 UTC).
       */
      public fun last_modified_time(last_modified_time: Long?): Builder {
        this.last_modified_time = last_modified_time
        return this
      }

      override fun build(): Modification = Modification(
        start_stop_selector = start_stop_selector,
        end_stop_selector = end_stop_selector,
        propagated_modification_delay = propagated_modification_delay,
        replacement_stops = replacement_stops,
        service_alert_id = service_alert_id,
        last_modified_time = last_modified_time,
        unknownFields = buildUnknownFields()
      )
    }

    public companion object {
      public const val DEFAULT_PROPAGATED_MODIFICATION_DELAY: Int = 0

      @JvmField
      public val ADAPTER: ProtoAdapter<Modification> = object : ProtoAdapter<Modification>(
        FieldEncoding.LENGTH_DELIMITED, 
        Modification::class, 
        "type.googleapis.com/transit_realtime.TripModifications.Modification", 
        PROTO_2, 
        null, 
        "xyz/ksharma/transport/gtfs_realtime.proto"
      ) {
        override fun encodedSize(`value`: Modification): Int {
          var size = value.unknownFields.size
          size += StopSelector.ADAPTER.encodedSizeWithTag(1, value.start_stop_selector)
          size += StopSelector.ADAPTER.encodedSizeWithTag(2, value.end_stop_selector)
          size += ProtoAdapter.INT32.encodedSizeWithTag(3, value.propagated_modification_delay)
          size += ReplacementStop.ADAPTER.asRepeated().encodedSizeWithTag(4,
              value.replacement_stops)
          size += ProtoAdapter.STRING.encodedSizeWithTag(5, value.service_alert_id)
          size += ProtoAdapter.UINT64.encodedSizeWithTag(6, value.last_modified_time)
          return size
        }

        override fun encode(writer: ProtoWriter, `value`: Modification) {
          StopSelector.ADAPTER.encodeWithTag(writer, 1, value.start_stop_selector)
          StopSelector.ADAPTER.encodeWithTag(writer, 2, value.end_stop_selector)
          ProtoAdapter.INT32.encodeWithTag(writer, 3, value.propagated_modification_delay)
          ReplacementStop.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.replacement_stops)
          ProtoAdapter.STRING.encodeWithTag(writer, 5, value.service_alert_id)
          ProtoAdapter.UINT64.encodeWithTag(writer, 6, value.last_modified_time)
          writer.writeBytes(value.unknownFields)
        }

        override fun encode(writer: ReverseProtoWriter, `value`: Modification) {
          writer.writeBytes(value.unknownFields)
          ProtoAdapter.UINT64.encodeWithTag(writer, 6, value.last_modified_time)
          ProtoAdapter.STRING.encodeWithTag(writer, 5, value.service_alert_id)
          ReplacementStop.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.replacement_stops)
          ProtoAdapter.INT32.encodeWithTag(writer, 3, value.propagated_modification_delay)
          StopSelector.ADAPTER.encodeWithTag(writer, 2, value.end_stop_selector)
          StopSelector.ADAPTER.encodeWithTag(writer, 1, value.start_stop_selector)
        }

        override fun decode(reader: ProtoReader): Modification {
          var start_stop_selector: StopSelector? = null
          var end_stop_selector: StopSelector? = null
          var propagated_modification_delay: Int? = null
          val replacement_stops = mutableListOf<ReplacementStop>()
          var service_alert_id: String? = null
          var last_modified_time: Long? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> start_stop_selector = StopSelector.ADAPTER.decode(reader)
              2 -> end_stop_selector = StopSelector.ADAPTER.decode(reader)
              3 -> propagated_modification_delay = ProtoAdapter.INT32.decode(reader)
              4 -> replacement_stops.add(ReplacementStop.ADAPTER.decode(reader))
              5 -> service_alert_id = ProtoAdapter.STRING.decode(reader)
              6 -> last_modified_time = ProtoAdapter.UINT64.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return Modification(
            start_stop_selector = start_stop_selector,
            end_stop_selector = end_stop_selector,
            propagated_modification_delay = propagated_modification_delay,
            replacement_stops = replacement_stops,
            service_alert_id = service_alert_id,
            last_modified_time = last_modified_time,
            unknownFields = unknownFields
          )
        }

        override fun redact(`value`: Modification): Modification = value.copy(
          start_stop_selector = value.start_stop_selector?.let(StopSelector.ADAPTER::redact),
          end_stop_selector = value.end_stop_selector?.let(StopSelector.ADAPTER::redact),
          replacement_stops = value.replacement_stops.redactElements(ReplacementStop.ADAPTER),
          unknownFields = ByteString.EMPTY
        )
      }

      private const val serialVersionUID: Long = 0L

      @JvmSynthetic
      public inline fun build(body: Builder.() -> Unit): Modification =
          Builder().apply(body).build()
    }
  }

  public class SelectedTrips(
    trip_ids: List<String> = emptyList(),
    /**
     * The ID of the new shape for the modified trips in this SelectedTrips.
     * May refer to a new shape added using a GTFS-RT Shape message, or to an existing shape defined
     * in the GTFS-Static feed’s shapes.txt.
     */
    @field:WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      schemaIndex = 1,
    )
    @JvmField
    public val shape_id: String? = null,
    unknownFields: ByteString = ByteString.EMPTY,
  ) : Message<SelectedTrips, SelectedTrips.Builder>(ADAPTER, unknownFields) {
    /**
     * A list of trips affected with this replacement that all have the same new `shape_id`.
     */
    @field:WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      label = WireField.Label.REPEATED,
      schemaIndex = 0,
    )
    @JvmField
    public val trip_ids: List<String> = immutableCopyOf("trip_ids", trip_ids)

    override fun newBuilder(): Builder {
      val builder = Builder()
      builder.trip_ids = trip_ids
      builder.shape_id = shape_id
      builder.addUnknownFields(unknownFields)
      return builder
    }

    override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is SelectedTrips) return false
      if (unknownFields != other.unknownFields) return false
      if (trip_ids != other.trip_ids) return false
      if (shape_id != other.shape_id) return false
      return true
    }

    override fun hashCode(): Int {
      var result = super.hashCode
      if (result == 0) {
        result = unknownFields.hashCode()
        result = result * 37 + trip_ids.hashCode()
        result = result * 37 + (shape_id?.hashCode() ?: 0)
        super.hashCode = result
      }
      return result
    }

    override fun toString(): String {
      val result = mutableListOf<String>()
      if (trip_ids.isNotEmpty()) result += """trip_ids=${sanitize(trip_ids)}"""
      if (shape_id != null) result += """shape_id=${sanitize(shape_id)}"""
      return result.joinToString(prefix = "SelectedTrips{", separator = ", ", postfix = "}")
    }

    public fun copy(
      trip_ids: List<String> = this.trip_ids,
      shape_id: String? = this.shape_id,
      unknownFields: ByteString = this.unknownFields,
    ): SelectedTrips = SelectedTrips(trip_ids, shape_id, unknownFields)

    public class Builder : Message.Builder<SelectedTrips, Builder>() {
      @JvmField
      public var trip_ids: List<String> = emptyList()

      @JvmField
      public var shape_id: String? = null

      /**
       * A list of trips affected with this replacement that all have the same new `shape_id`.
       */
      public fun trip_ids(trip_ids: List<String>): Builder {
        checkElementsNotNull(trip_ids)
        this.trip_ids = trip_ids
        return this
      }

      /**
       * The ID of the new shape for the modified trips in this SelectedTrips.
       * May refer to a new shape added using a GTFS-RT Shape message, or to an existing shape
       * defined in the GTFS-Static feed’s shapes.txt.
       */
      public fun shape_id(shape_id: String?): Builder {
        this.shape_id = shape_id
        return this
      }

      override fun build(): SelectedTrips = SelectedTrips(
        trip_ids = trip_ids,
        shape_id = shape_id,
        unknownFields = buildUnknownFields()
      )
    }

    public companion object {
      @JvmField
      public val ADAPTER: ProtoAdapter<SelectedTrips> = object : ProtoAdapter<SelectedTrips>(
        FieldEncoding.LENGTH_DELIMITED, 
        SelectedTrips::class, 
        "type.googleapis.com/transit_realtime.TripModifications.SelectedTrips", 
        PROTO_2, 
        null, 
        "xyz/ksharma/transport/gtfs_realtime.proto"
      ) {
        override fun encodedSize(`value`: SelectedTrips): Int {
          var size = value.unknownFields.size
          size += ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(1, value.trip_ids)
          size += ProtoAdapter.STRING.encodedSizeWithTag(2, value.shape_id)
          return size
        }

        override fun encode(writer: ProtoWriter, `value`: SelectedTrips) {
          ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 1, value.trip_ids)
          ProtoAdapter.STRING.encodeWithTag(writer, 2, value.shape_id)
          writer.writeBytes(value.unknownFields)
        }

        override fun encode(writer: ReverseProtoWriter, `value`: SelectedTrips) {
          writer.writeBytes(value.unknownFields)
          ProtoAdapter.STRING.encodeWithTag(writer, 2, value.shape_id)
          ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 1, value.trip_ids)
        }

        override fun decode(reader: ProtoReader): SelectedTrips {
          val trip_ids = mutableListOf<String>()
          var shape_id: String? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> trip_ids.add(ProtoAdapter.STRING.decode(reader))
              2 -> shape_id = ProtoAdapter.STRING.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return SelectedTrips(
            trip_ids = trip_ids,
            shape_id = shape_id,
            unknownFields = unknownFields
          )
        }

        override fun redact(`value`: SelectedTrips): SelectedTrips = value.copy(
          unknownFields = ByteString.EMPTY
        )
      }

      private const val serialVersionUID: Long = 0L

      @JvmSynthetic
      public inline fun build(body: Builder.() -> Unit): SelectedTrips =
          Builder().apply(body).build()
    }
  }
}
