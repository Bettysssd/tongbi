public List<BidItem> filterValid(List<BidItem> items) {
    List<BidItem> result = new ArrayList<BidItem>();
    for (int i = 0; i < items.size(); i++) {
        BidItem item = items.get(i);
        if (item.getPrice() != null) {
            if (item.getPrice().doubleValue() > 0
                    && item.getStatus().equals("1")) {
                result.add(item);
            }
        }
    }
    return result;
}
