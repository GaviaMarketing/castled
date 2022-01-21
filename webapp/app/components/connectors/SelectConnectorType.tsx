import React, { useEffect, useState } from "react";
import { ConnectorTypeDto } from "@/app/common/dtos/ConnectorTypeDto";
import { Badge, Col, ListGroup, Row } from "react-bootstrap";
import { ConnectorCategory } from "@/app/common/utils/types";
import appsService from "@/app/services/appsService";
import warehouseService from "@/app/services/warehouseService";
import { usePipelineWizContext } from "@/app/common/context/pipelineWizardContext";

export interface SelectConnectorTypeProps {
  category: ConnectorCategory;
  onSelect: (type: ConnectorTypeDto) => void;
}

const SelectConnectorType = ({
  category,
  onSelect,
}: SelectConnectorTypeProps) => {
  const [typeList, setTypeList] = useState<ConnectorTypeDto[] | undefined>();
  const { pipelineWizContext } = usePipelineWizContext();
  useEffect(() => {
    const fetcher =
      category === "App" ? appsService.types : warehouseService.types;
    fetcher().then(({ data }) => {
      setTypeList(data);
    });
  }, [category]);
  return (
    <>
      {pipelineWizContext?.isDemo && category === "App" && (
        <div className="help-message">
          <Badge bg="warning" className="badge badge-warning">
            Google Sheets is the most used app for testing demo pipelines.
          </Badge>
        </div>
      )}
      <div className="grid-categories">
        <Row xs={3}>
          {typeList?.map((type, i) => (
            <ListGroup key={i}>
              <ListGroup.Item
                className="rounded"
                onClick={() => onSelect(type)}
              >
                <Col>
                  <div>
                    <img className={type.title} src={type.logoUrl}></img>
                    <strong>{type.title} </strong>
                    {type.count > 0 && (
                      <Badge bg="secondary">{type.count}</Badge>
                    )}
                  </div>
                </Col>
              </ListGroup.Item>
            </ListGroup>
          ))}
        </Row>
      </div>
    </>
  );
};

export default SelectConnectorType;
