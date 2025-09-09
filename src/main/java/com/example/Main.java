package com.example;

import com.example.api.ElpriserAPI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {


        if (args.length == 0) {
            Help();
            return;
        }


        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help")) {
                Help();
                return;
            }
        }


        String zon = null;
        String datum = null;
        boolean sorterad = false;
        String laddning = null;


        for (int i = 0; i < args.length; i++) {
            String command = args[i];

            if (command.equals("--zone")) {

                if (i + 1 < args.length) {
                    zon = args[i + 1];
                    i++;
                }
            } else if (command.equals("--date")) {

                if (i + 1 < args.length) {
                    datum = args[i + 1];
                    i++;
                }
            } else if (command.equals("--sorted")) {
                sorterad = true;
            } else if (command.equals("--charging")) {

                if (i + 1 < args.length) {
                    laddning = args[i + 1];
                    i++;
                }
            }
        }


        if (zon == null) {
            System.out.println("Ogiltig zon: Zon måste anges. Använd --zone SE1/SE2/SE3/SE4");
            Help();
            return;
        }


        if (!zon.equals("SE1") && !zon.equals("SE2") && !zon.equals("SE3") && !zon.equals("SE4")) {
            System.out.println("Ogiltig zon: " + zon + ". Använd SE1, SE2, SE3 eller SE4");
            return;
        }


        LocalDate valtDatum;
        if (datum == null) {

            valtDatum = LocalDate.now();
        } else {

            try {
                valtDatum = LocalDate.parse(datum);
            } catch (Exception e) {
                System.out.println("Ogiltigt datum: " + datum + ". Använd formatet YYYY-MM-DD");
                return;
            }
        }


        ElpriserAPI api = new ElpriserAPI(false);
        ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zon);
        List<ElpriserAPI.Elpris> priser = api.getPriser(valtDatum, prisklass);


        if (priser.isEmpty()) {
            System.out.println("Inga priser hittades för " + valtDatum + " i zon " + zon);
            return;
        }


        if (laddning != null) {
            hittaBastaLaddningstid(priser, laddning, valtDatum, api, zon);
        } else if (sorterad) {
            visaSorteradePriser(priser);
        } else {
            visaPrisInformation(priser);
        }
    }


    private static void Help() {
        System.out.println("Usage: java Main [options]");
        System.out.println("Användning: java Main [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --zone SE1|SE2|SE3|SE4    Välj elpriszon (måste anges)");
        System.out.println("  --date YYYY-MM-DD         Välj datum (idag om inget anges)");
        System.out.println("  --sorted                  Visa priser sorterade från billigast till dyrast");
        System.out.println("  --charging 2h|4h|8h       Hitta bästa tid att ladda elbil");
        System.out.println("  --help                    Visa denna hjälp");
        System.out.println();
        System.out.println("Zoner: SE1, SE2, SE3, SE4");
        System.out.println();
        System.out.println("Exempel:");
        System.out.println("  java Main --zone SE3 --date 2025-09-04");
        System.out.println("  java Main --zone SE1 --sorted");
        System.out.println("  java Main --zone SE2 --charging 4h");
    }


    private static void visaPrisInformation(List<ElpriserAPI.Elpris> priser) {

        double billigastPris = 1000000;
        double dyrastPris = -1000000;
        double totaltPris = 0;
        LocalTime billigastTid = null;
        LocalTime dyrastTid = null;


        for (ElpriserAPI.Elpris pris : priser) {

            double ore = pris.sekPerKWh() * 100;
            totaltPris = totaltPris + ore;


            if (ore < billigastPris) {
                billigastPris = ore;
                billigastTid = pris.timeStart().toLocalTime();
            }


            if (ore > dyrastPris) {
                dyrastPris = ore;
                dyrastTid = pris.timeStart().toLocalTime();
            }
        }


        double medelPris = totaltPris / priser.size();


        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("sv-SE"));
        DecimalFormat formaterare = new DecimalFormat("0.00", symbols);


        System.out.println("Sammanfattning:");
        System.out.println("Lägsta pris: " + formateraTid(billigastTid) + " - " + formaterare.format(billigastPris) + " öre");
        System.out.println("Högsta pris: " + formateraTid(dyrastTid) + " - " + formaterare.format(dyrastPris) + " öre");
        System.out.println("Medelpris: " + formaterare.format(medelPris) + " öre");
        System.out.println();


        System.out.println("Alla priser:");
        for (ElpriserAPI.Elpris pris : priser) {
            double ore = pris.sekPerKWh() * 100;
            LocalTime start = pris.timeStart().toLocalTime();
            LocalTime end = pris.timeEnd().toLocalTime();
            System.out.println(formateraTidRange(start, end) + " " + formaterare.format(ore) + " öre");
        }
    }


    private static void visaSorteradePriser(List<ElpriserAPI.Elpris> priser) {

        List<ElpriserAPI.Elpris> sorteradLista = new ArrayList<ElpriserAPI.Elpris>();
        for (ElpriserAPI.Elpris pris : priser) {
            sorteradLista.add(pris);
        }


        Collections.sort(sorteradLista, new Comparator<ElpriserAPI.Elpris>() {
            public int compare(ElpriserAPI.Elpris a, ElpriserAPI.Elpris b) {
                if (a.sekPerKWh() < b.sekPerKWh()) {
                    return -1;
                } else if (a.sekPerKWh() > b.sekPerKWh()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });


        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("sv-SE"));
        DecimalFormat formaterare = new DecimalFormat("0.00", symbols);


        System.out.println("Priser sorterade från billigast till dyrast:");
        for (ElpriserAPI.Elpris pris : sorteradLista) {
            double ore = pris.sekPerKWh() * 100;
            LocalTime start = pris.timeStart().toLocalTime();
            LocalTime end = pris.timeEnd().toLocalTime();
            System.out.println(formateraTidRange(start, end) + " " + formaterare.format(ore) + " öre");
        }
    }


    private static void hittaBastaLaddningstid(List<ElpriserAPI.Elpris> dagensPriser, String laddningstid,
                                               LocalDate startDatum, ElpriserAPI api, String zon) {

        int antalTimmar;
        if (laddningstid.equals("2h")) {
            antalTimmar = 2;
        } else if (laddningstid.equals("4h")) {
            antalTimmar = 4;
        } else if (laddningstid.equals("8h")) {
            antalTimmar = 8;
        } else {
            System.out.println("Fel: " + laddningstid + " är inte giltig. Använd 2h, 4h eller 8h");
            return;
        }


        List<ElpriserAPI.Elpris> allaPriser = new ArrayList<ElpriserAPI.Elpris>();
        for (ElpriserAPI.Elpris pris : dagensPriser) {
            allaPriser.add(pris);
        }


        LocalDate morgondagen = startDatum.plusDays(1);
        List<ElpriserAPI.Elpris> morgondagensPriser = api.getPriser(morgondagen, ElpriserAPI.Prisklass.valueOf(zon));
        for (ElpriserAPI.Elpris pris : morgondagensPriser) {
            allaPriser.add(pris);
        }


        double bastaMedelpriset = 1000000;
        int bastaStartIndex = -1;


        for (int startIndex = 0; startIndex <= allaPriser.size() - antalTimmar; startIndex++) {
            double summa = 0;


            for (int i = 0; i < antalTimmar; i++) {
                double pris = allaPriser.get(startIndex + i).sekPerKWh();
                summa = summa + pris;
            }


            double medelpris = summa / antalTimmar;


            if (medelpris < bastaMedelpriset) {
                bastaMedelpriset = medelpris;
                bastaStartIndex = startIndex;
            }
        }


        if (bastaStartIndex == -1) {
            System.out.println("Kunde inte hitta någon bra laddningstid på " + antalTimmar + " timmar.");
            return;
        }


        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("sv-SE"));
        DecimalFormat formaterare = new DecimalFormat("0.00", symbols);


        LocalTime startTid = allaPriser.get(bastaStartIndex).timeStart().toLocalTime();


        System.out.println("Bästa tiden att ladda " + antalTimmar + " timmar:");
        System.out.println("Påbörja laddning: kl " + startTid.toString().replace(":00", "") + ":00");
        System.out.println("Medelpris för fönster: " + formaterare.format(bastaMedelpriset * 100) + " öre");


        System.out.print("Timmar: ");
        for (int i = 0; i < antalTimmar; i++) {
            LocalTime tid = allaPriser.get(bastaStartIndex + i).timeStart().toLocalTime();
            System.out.print(tid.toString().replace(":00", ""));
            if (i < antalTimmar - 1) {
                System.out.print(", ");
            }
        }
        System.out.println();
    }


    private static String formateraTid(LocalTime tid) {
        if (tid == null) {
            return "N/A";
        }
        int timme = tid.getHour();
        return String.format("%02d-%02d", timme, timme + 1);
    }


    private static String formateraTidRange(LocalTime start, LocalTime end) {
        return String.format("%02d-%02d", start.getHour(), end.getHour());
    }
}


