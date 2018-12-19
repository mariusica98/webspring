package webSpring;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections4.ResettableIterator;
import org.aspectj.weaver.tools.cache.AsynchronousFileCacheBacking.ClearCommand;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.DeleteEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Text;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import webSpring.entity.Malicious;
import webSpring.entity.Person;
import webSpring.model.PersonViewModel;
import webSpring.repository.MaliciousRepository;
import webSpring.repository.PersonRepository;

@Controller
public class PersonController {
	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private MaliciousRepository maliciousRepository;

	@Autowired
	private ServletContext servletContext;

	@GetMapping("/person")
	public String greetingForm(Model model, HttpServletResponse response) {
		model.addAttribute("personViewModel", new PersonViewModel());
		return "insert";
	}

	@PostMapping("/person")
	public String formProcess(Model model,@ModelAttribute PersonViewModel personViewModel, @RequestParam("submit") String reqParam,
			HttpServletResponse response, HttpSession session) {
		List<Person> searchedPersons = new ArrayList<>();
		List<Malicious> maliciousPersons = new ArrayList<>();

		switch (reqParam) {
		case "Search":
			String searchedName = personViewModel.getName().trim();
			if (!searchedName.equals("")) {
				Iterable<Person> persons = personRepository.findAll();
				for (Person person : persons) {
					if (person.getName().toUpperCase().contains(searchedName.toUpperCase())) {
						searchedPersons.add(person);
					}
				}
				personViewModel.setSearchedList(searchedPersons);

				Iterable<Malicious> malicious = maliciousRepository.findAll();
				for (Malicious person : malicious) {
					if (person.getLine().toUpperCase().contains(searchedName.toUpperCase())) {
						maliciousPersons.add(person);
					}
				}
				personViewModel.setMaliciousList(maliciousPersons);

				exportToPDF(searchedPersons, maliciousPersons, "output/txt.pdf", searchedName);
			}
			return "insert";

		case "Download":
			loadToDatabase("https://www.bis.doc.gov/dpl/dpl.txt");
			return "insert";
			
		case "Reset":
			personViewModel.setName("");
			personViewModel.setSearchedList(new ArrayList<>());
			return "insert";
		
		case "Export":
			try {
				downloadFile(response, "output/txt.pdf");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "insert";

		default:
			return "insert";
		}
	}

	private void exportToPDF(List<Person> searchedPersons, List<Malicious> maliciousPersons, String fileName,
			String searchedName) {
		Document pdfDoc = new Document(PageSize.A4,13,13,100,90);

		Font cellFontBold = FontFactory.getFont("Times Roman", 8, BaseColor.BLACK);
		cellFontBold.setStyle(Font.BOLD);

		Font cellFont = FontFactory.getFont("Times Roman", 8, BaseColor.BLACK);
		Font textFont = FontFactory.getFont("Times Roman", 14, BaseColor.BLACK);
		textFont.setStyle(Font.BOLD);

		try {
			HeaderFooterPageEvent event = new HeaderFooterPageEvent();
			PdfWriter.getInstance(pdfDoc, new FileOutputStream(fileName)).setPageEvent(event);
			
			pdfDoc.open();

			Paragraph title = new Paragraph("Information about: " + searchedName, textFont);
			title.setSpacingBefore(60f);
			title.setSpacingAfter(30f);
			pdfDoc.add(title);
			

			if (!searchedPersons.isEmpty()) {
				PdfPTable table = new PdfPTable(12);
				table.setWidths(new int[] { 10, 10, 5, 3, 3, 5, 5, 5, 3, 4, 8, 8 });
				table.setWidthPercentage(100);
				
				table.addCell(setCell(cellFontBold, "NAME"));
				table.addCell(setCell(cellFontBold, "STREET ADDRESS"));
				table.addCell(setCell(cellFontBold, "CITY"));
				table.addCell(setRotatedCell(cellFontBold, "STATE"));
				table.addCell(setRotatedCell(cellFontBold, "COUNTRY"));
				table.addCell(setRotatedCell(cellFontBold, "POSTAL CODE"));
				table.addCell(setRotatedCell(cellFontBold, "EFFECTIVE DATE"));
				table.addCell(setRotatedCell(cellFontBold, "EXPIRATION DATE"));
				table.addCell(setRotatedCell(cellFontBold, "STANDARD ORDER"));
				table.addCell(setRotatedCell(cellFontBold, "LAST UPDATE"));
				table.addCell(setCell(cellFontBold, "ACTION"));
				table.addCell(setRotatedCell(cellFontBold, "FR. CITATION"));

				for (Person person : searchedPersons) {
					for (String s : person.toString().split("###")) {
						table.addCell(setCell(cellFont, s));
					}
				}
				table.setHeaderRows(1);
				pdfDoc.add(table);
			}

			if (!maliciousPersons.isEmpty()) {
				PdfPTable table = new PdfPTable(1);
				table.setWidthPercentage(100);
				table.setSpacingBefore(30f);
				table.setSpacingAfter(30f);

				table.addCell(setCell(cellFontBold,
						"This search offer also some unformatted information for: " + searchedName));

				for (Malicious person : maliciousPersons) {
					Paragraph docTitle = new Paragraph(person.getLine(), cellFont);
					PdfPCell cell = new PdfPCell(docTitle);
					cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
					cell.setHorizontalAlignment(Element.ALIGN_LEFT);
					table.addCell(cell);
				}
				pdfDoc.add(table);
			}
			

		} catch (FileNotFoundException | DocumentException e) {
			e.printStackTrace();
		} finally {
			pdfDoc.close();
		}
	}

	private PdfPCell setCell(Font cellFontBold, String text) {
		Paragraph p = new Paragraph(text, cellFontBold);
		PdfPCell cell = new PdfPCell(p);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		return cell;
	}

	private PdfPCell setRotatedCell(Font cellFontBold, String text) {
		PdfPCell cell = setCell(cellFontBold, text);
		cell.setRotation(90);
		return cell;
	}

	private void loadToDatabase(String linkForDenied) {
		URL deniedPersonListURL;
		BufferedReader in = null;
		URLConnection yc;
		personRepository.deleteAll();
		maliciousRepository.deleteAll();

		try {
			deniedPersonListURL = new URL(linkForDenied);
			yc = deniedPersonListURL.openConnection();
			in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

			String strLine;
			while ((strLine = in.readLine()) != null) {
				String s1 = strLine.replaceAll(" \"", "\"");
				String s2 = s1.replaceAll("\" ", "\"");
				String s3 = s2.replaceAll("\"\"", "\" \"");

				String[] splited = s3.split("\"*\"");

				if (splited.length == 24) {
					Person person = new Person();
					person.setName(splited[1]);
					person.setStreetAddress(splited[3]);
					person.setCity(splited[5]);
					person.setState(splited[7]);
					person.setCountry(splited[9]);
					person.setPostalCode(splited[11]);
					person.setEffectiveDate(splited[13]);
					person.setExpirationDate(splited[15]);
					person.setStandardOrder(splited[17]);
					person.setLastUpdate(splited[19]);
					person.setAction(splited[21]);
					person.setFrCitation(splited[23]);

					personRepository.save(person);
				} else {
					Malicious malicious = new Malicious();
					malicious.setLine(strLine);

					maliciousRepository.save(malicious);
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private MediaType getMediaTypeForFileName(String fileName) {
		String mineType = servletContext.getMimeType(fileName);

		try {
			MediaType mediaType = MediaType.parseMediaType(mineType);
			return mediaType;
		} catch (Exception e) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	private void downloadFile(HttpServletResponse response, String fileName) throws IOException {
		MediaType mediaType = getMediaTypeForFileName(fileName);
		File file = new File(fileName);

		response.setContentType(mediaType.getType()); // Content-Type: application/pdf
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName());
		response.setContentLength((int) file.length());

		BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(file));
		BufferedOutputStream outStream = new BufferedOutputStream(response.getOutputStream());

		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		while ((bytesRead = inStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}

		outStream.flush();
		outStream.close();
		inStream.close();
	}
}