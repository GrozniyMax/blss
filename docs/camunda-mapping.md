# Перенос бизнес-процессов на Camunda

## Процессы

В исходной BPMN-модели были выделены три исполняемых сценария:

- создание товара на складе;
- создание заказа с проверкой данных, бронированием остатков и созданием позиций заказа;
- выдача заказа с проверкой готовности, решением покупателя и ветками выдачи/возврата.

В Camunda они оформлены как отдельные executable BPMN 2.0 процессы:

- `warehouse-product-create` - `src/main/resources/bpmn/warehouse-product-create.bpmn`;
- `warehouse-order-create` - `src/main/resources/bpmn/warehouse-order-create.bpmn`;
- `warehouse-order-pickup` - `src/main/resources/bpmn/warehouse-order-pickup.bpmn`.

## Интеграция со standalone Camunda

Движок Camunda не встраивается в приложение. Приложение подключается к standalone engine через:

- REST API Camunda: `camunda.rest-url`;
- External Task Client: `org.camunda.bpm:camunda-external-task-client`.

REST-адаптер для старта процессов находится в `com.blss.blss.camunda.CamundaProcessClient`.
Worker'ы external tasks находятся в `com.blss.blss.camunda.CamundaExternalTaskWorkers`.

## Topics external tasks

- `product.create` - вызывает `StoreService.createProduct(...)`;
- `order.create` - вызывает `OrderService.createOrder(...)`;
- `order.pickup.verify` - проверяет, что заказ в статусе `READY_FOR_PICKUP`;
- `order.pickup.complete` - переводит заказ в `DONE`;
- `order.pickup.reject` - переводит заказ в `CANCELED`.

## Формы Camunda

Формы для Camunda Tasklist/Modeler лежат в `src/main/resources/forms`:

- `product-create-form.form`;
- `order-create-form.form`;
- `pickup-start-form.form`;
- `pickup-inspection-form.form`.

Для запуска через REST API приложения добавлены endpoints:

- `POST /camunda/processes/products`;
- `POST /camunda/processes/orders`;
- `POST /camunda/processes/pickup`.

Перед использованием процессы и формы нужно задеплоить в standalone Camunda через Camunda Modeler или REST API engine.
