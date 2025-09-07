@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final StripeService stripeService;

    public PaymentController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, Object>> createPaymentIntent(@RequestBody Map<String, Object> request) {
        try {
            Long amount = ((Number) request.get("amount")).longValue();
            PaymentIntent intent = stripeService.createPaymentIntent(amount, "myr");

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
