package crptapi;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final StopWatch _sw = new StopWatch();
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit);
        }, 0, 1, timeUnit);
    }

    public void createDocument(Object document, String signature) {
        try {
            if (semaphore.tryAcquire()) {
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {выше ты мне 
                    final HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");

                    ObjectMapper ow = new ObjectMapper();
                    ow.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
                    String json = ow.writeValueAsString(document);

                    StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(entity);

                    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode == 200) {
                            System.out.println("Success");
                        } else {
                            System.out.println("Failed. Status code: " + statusCode);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                }
            } else {
                System.out.println("Request limit reached. Please try again later.");
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            System.out.println("Thread was interrupted. " + e.getMessage());
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }

    
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 3);

        Description description = new Description("partInn");

        List<Product> products = getProducts();

        Document document = new Document(description, "id", "status", "LP_INTRODUCE_GOODS", true, "owner_inn", "participant_inn", "producer_inn", "production_type", "2020-01-23", products, "2020-01-23", "reg_number");
        String signature = "signature";
        
        try {
            crptApi.createDocument(document, signature);
        } finally {
            crptApi.shutdown();
        }
    }

    private static List<Product> getProducts() {
        Product product1 = new Product("cert1", "2020-01-23", "num1", "owner1", "prod1", "2020-01-23", "tnved1", "uit1", "uitu1");
        Product product2 = new Product("cert2", "2020-01-23", "num2", "owner2", "prod2", "2020-01-23", "tnved2", "uit2", "uitu2");
        Product product3 = new Product("cert3", "2020-01-23", "num3", "owner3", "prod3", "2020-01-23", "tnved3", "uit3", "uitu3");
        List<Product> products = new ArrayList<>();
        products.add(product1);
        products.add(product2);
        products.add(product3);
        return products;
    }

}

class Document {

    private Description description;
    private String doc_id;
    private String doc_status;
    private String doc_type;
    private boolean importRequest = true;
    private String owner_inn;
    private String participant_inn;
    private String producer_inn;
    private String production_date;
    private String production_type;
    private List<Product> products;
    private String reg_date;
    private String reg_number;

    public Document(Description description, String doc_id, String doc_status, String doc_type, boolean importRequest, String owner_inn, String participant_inn, String producer_inn, String production_date, String production_type, List<Product> products, String reg_date, String reg_number) {
        this.description = description;
        this.doc_id = doc_id;
        this.doc_status = doc_status;
        this.doc_type = doc_type;
        this.importRequest = importRequest;
        this.owner_inn = owner_inn;
        this.participant_inn = participant_inn;
        this.producer_inn = producer_inn;
        this.production_date = production_date;
        this.production_type = production_type;
        this.products = products;
        this.reg_date = reg_date;
        this.reg_number = reg_number;
    }
}

class Description {
    private String participantInn;

    public Description(String participantInn) {
        this.participantInn = participantInn;
    }
}

class Product {
    private String certificate_document;
    private String certificate_document_date;
    private String certificate_document_number;
    private String owner_inn;
    private String producer_inn;
    private String production_date;
    private String tnved_code;
    private String uit_code;
    private String uitu_code;

    public Product(String certificate_document, String certificate_document_date, String certificate_document_number, String owner_inn, String producer_inn, String production_date, String tnved_code, String uit_code, String uitu_code) {
        this.certificate_document = certificate_document;
        this.certificate_document_date = certificate_document_date;
        this.certificate_document_number = certificate_document_number;
        this.owner_inn = owner_inn;
        this.producer_inn = producer_inn;
        this.production_date = production_date;
        this.tnved_code = tnved_code;
        this.uit_code = uit_code;
        this.uitu_code = uitu_code;
    }
}