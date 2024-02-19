(function (Granite, $) {
    'use strict';

    $(document).ready(function () {
        const eventSource = new EventSource("/services/backpack/sse");
        const container = document.getElementsByClassName('foundation-layout-panel-content')[0];
        eventSource.onmessage = (message) => {
            container.append(message.data);
        }
    })

})(Granite, Granite.$);
