package org.mtransit.parser.ca_joliette_ctjm_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// https://exo.quebec/en/about/open-data
// https://exo.quebec/xdata/crtl/google_transit.zip
public class JolietteCTJMBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-joliette-ctjm-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new JolietteCTJMBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating CTJM bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating CTJM bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		//noinspection deprecation
		if (!"CRTL".equals(gRoute.getAgencyId())) {
			return true; // exclude wrong agency
		}
		//noinspection deprecation
		if (gRoute.getRouteId().length() > 1) {
			return true; // exclude CRTL Lanaudière Bus
		}
		return super.excludeRoute(gRoute);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "00693F";

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_C61D23 = "C61D23";
	private static final String COLOR_1263B0 = "1263B0";
	private static final String COLOR_D69732 = "D69732";

	private static final String RSN_1 = "1";
	private static final String RSN_3 = "3";
	private static final String RSN_4 = "4";

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (RSN_1.equals(gRoute.getRouteShortName())) return COLOR_D69732;
		if (RSN_3.equals(gRoute.getRouteShortName())) return COLOR_1263B0;
		if (RSN_4.equals(gRoute.getRouteShortName())) return COLOR_C61D23;
		return super.getRouteColor(gRoute);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);
	private static final String DIRECTION_REPLACEMENT = "";

	private static final Pattern SECTEUR = Pattern.compile("(secteur[s]? )", Pattern.CASE_INSENSITIVE);
	private static final String SECTEUR_REPLACEMENT = "";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = DIRECTION.matcher(tripHeadsign).replaceAll(DIRECTION_REPLACEMENT);
		tripHeadsign = SECTEUR.matcher(tripHeadsign).replaceAll(SECTEUR_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s", mTrip, mTripToMerge);
	}

	private static final Pattern START_WITH_FACE_A = Pattern.compile("^(face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE_AU = Pattern.compile("^(face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern START_WITH_FACE = Pattern.compile("^(face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern SPACE_FACE_A = Pattern.compile("( face à )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE_AU = Pattern.compile("( face au )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern SPACE_WITH_FACE = Pattern.compile("( face )", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private static final Pattern[] START_WITH_FACES = new Pattern[]{START_WITH_FACE_A, START_WITH_FACE_AU, START_WITH_FACE};

	private static final Pattern[] SPACE_FACES = new Pattern[]{SPACE_FACE_A, SPACE_WITH_FACE_AU, SPACE_WITH_FACE};

	private static final Pattern AVENUE = Pattern.compile("( avenue)", Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = " av.";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = AVENUE.matcher(gStopName).replaceAll(AVENUE_REPLACEMENT);
		gStopName = Utils.replaceAll(gStopName, START_WITH_FACES, CleanUtils.SPACE);
		gStopName = Utils.replaceAll(gStopName, SPACE_FACES, CleanUtils.SPACE);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	private static final String ZERO = "0";

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		if (ZERO.equals(gStop.getStopCode())) {
			return EMPTY;
		}
		return super.getStopCode(gStop);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String RDP = "RDP";
	private static final String SFV = "SFV";
	private static final String SMS = "SMS";

	private static final String A = "A";
	private static final String B = "B";
	private static final String D = "D";

	@Override
	public int getStopId(@NotNull GStop gStop) {
		String stopCode = getStopCode(gStop);
		if (stopCode.length() > 0) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		//noinspection deprecation
		final String stopId1 = gStop.getStopId();
		Matcher matcher = DIGITS.matcher(stopId1);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			int stopId;
			if (stopId1.startsWith(RDP)) {
				stopId = 100_000;
			} else if (stopId1.startsWith(SFV)) {
				stopId = 200_000;
			} else if (stopId1.startsWith(SMS)) {
				stopId = 300_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (start with)! " + gStop);
			}
			if (stopId1.endsWith(A)) {
				stopId += 1_000;
			} else if (stopId1.endsWith(B)) {
				stopId += 2_000;
			} else if (stopId1.endsWith(D)) {
				stopId += 4_000;
			} else {
				throw new MTLog.Fatal("Stop doesn't have an ID (end with)! " + gStop);
			}
			return stopId + digits;
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}
}
